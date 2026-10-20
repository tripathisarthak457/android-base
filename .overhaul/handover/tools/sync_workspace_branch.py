#!/usr/bin/env python3
"""sync_workspace_branch.py: back up the .overhaul workspace to the orphan branch `overhaul-workspace` of the PUBLIC
android-base repo (HANDOVER.md section 6.1 #6). Native-Windows port of sync-workspace-branch.sh, which needed rsync.
The orchestrator runs it; agents inside workflows never do.

    python -I sync_workspace_branch.py                     copy and redact into the worktree; nothing is staged
    python -I sync_workspace_branch.py --dry-run           also stage, run every guard, summarise, then unstage
    python -I sync_workspace_branch.py --commit [--push] [--message "subject"]

What goes into the branch:
    .overhaul/          the workspace, filtered by .overhaul/handover/backup-include.txt (rsync filter rules, evaluated
                        with rsync's own matching rules); files over 50 MiB are skipped
    .overhaul/handover/ the whole folder except local-only/ (never published) and claude-memory/ (it goes to the root)
    README.md           from .overhaul/handover/BRANCH-README.md
    CLAUDE.md, HANDOVER.md, tools-CLAUDE.md   from .overhaul/handover/; tools-CLAUDE.md is first refreshed from the
                        live tools-root CLAUDE.md so the branch always carries the current one
    claude-memory/      the *.md files of this project's live Claude memory folder

The working copy is a persistent git worktree of the local branch at .overhaul/wt/_workspace-branch; the filter's
"- /wt/" keeps it out of its own copy. Like rsync -a without --delete, nothing in the branch is ever deleted. Only files
whose bytes differ are rewritten. The push is a plain fast-forward: never --force, never main.
Safe to re-run: with nothing changed it commits nothing.
"""

import argparse
import datetime
import fnmatch
import os
import re
import stat
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parents[2]
OV = TOOLS / '.overhaul'
H = OV / 'handover'
AB = TOOLS / 'android-base'
WT = OV / 'wt' / '_workspace-branch'
FILTER_FILE = H / 'backup-include.txt'
TERMS_FILE = H / 'local-only' / 'never-publish-terms.txt'
LIVE_TOOLS_CLAUDE = TOOLS / 'CLAUDE.md'
# Claude Code names a project's folder after its path with every non-alphanumeric character turned into '-'.
MEM_SRC = Path.home() / '.claude' / 'projects' / re.sub(r'[^A-Za-z0-9]', '-', str(TOOLS)) / 'memory'
AB_URL = 'https://github.com/tripathisarthak457/android-base.git'
BRANCH = 'overhaul-workspace'
# rsync's --max-size=50M: its suffixes are powers of 1024, and only strictly larger files are skipped.
MAX_SIZE = 50 * 1024 * 1024
TRAILER = 'Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>'

# autocrlf would rewrite line endings on the way into the index; the branch must get the workspace's bytes.
GIT = ['git', '-c', 'core.autocrlf=false', '-c', 'core.quotepath=false']

# Local dev-only values (a throwaway local Postgres and a local API; PUSHED.md section 1.5). The character classes are
# the shell version's sed expressions; '\n' is excluded where sed's line-at-a-time matching excluded it implicitly.
R = b'<local-dev-value>'
REDACT_TARGETS = ('.overhaul/handoff/WP09.md', '.overhaul/out/WP10/run-api.sh', '.overhaul/handover/MANIFEST.md')
REDACTIONS = (
    (re.compile(rb'''POSTGRES_PASSWORD=[^<\s"'`,;)]+'''), b'POSTGRES_PASSWORD=' + R),
    (re.compile(rb'''(postgres(ql)?://[^:/@\s"']+):[^@<\s"']+@'''), rb'\1:' + R + b'@'),
    (re.compile(rb"ADMIN_TOKEN='[^'<\n]*'"), b"ADMIN_TOKEN='" + R + b"'"),
    (re.compile(rb"IP_SALT='[^'<\n]*'"), b"IP_SALT='" + R + b"'"),
)
LEFTOVER = re.compile(rb'''postgres(ql)?://[^:/@\s"']+:[^@<\s"']+@|POSTGRES_PASSWORD=[^<\s"']''')
# Both backup scripts spell these patterns out, so they would always trip the leftover check.
LEFTOVER_SKIP_NAMES = {'sync-workspace-branch.sh', 'sync_workspace_branch.py'}

SECRET_NAMES = re.compile(
    r'\.(jks|keystore|p12|pfx|pem|key)$|(^|/)google-services\.json$|GoogleService-Info\.plist$|(^|/)local\.properties$'
    r'|keystore\.properties$|(^|/)\.env($|\.)|recovery-codes|PeriFlow-Postman|\.jsonl$|/gmail/', re.I)
SECRET_TEXT = re.compile(
    rb'ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|gh[osur]_[A-Za-z0-9]{30,}|sk-ant-[A-Za-z0-9_-]{8,}'
    rb'|sk-[A-Za-z0-9_-]{20,}|AIza[0-9A-Za-z_-]{30,}|AKIA[0-9A-Z]{16}|xox[abposr]-[A-Za-z0-9-]{10,}'
    rb'|-----BEGIN [A-Z ]*PRIVATE KEY-----|eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.')


def say(msg):
    print(f'sync_workspace_branch: {msg}', flush=True)


def warn(msg):
    print(f'sync_workspace_branch: WARNING: {msg}', file=sys.stderr, flush=True)


def die(msg):
    print(f'sync_workspace_branch: {msg}', file=sys.stderr, flush=True)
    sys.exit(1)


def git(*args, cwd=WT, check=True, stdin=None):
    p = subprocess.run([*GIT, '-C', str(cwd), *args], input=stdin, capture_output=True)
    if check and p.returncode != 0:
        err = p.stderr.decode('utf-8', 'replace').strip()
        die(f"git {' '.join(args)} failed with exit code {p.returncode}" + (f':\n{err}' if err else ''))
    return p


def out(p):
    return p.stdout.decode('utf-8', 'surrogateescape').strip()


def lower_priority():
    """The shell version ran rsync and the push under nice -n 19; children inherit a below-normal priority class."""
    try:
        if os.name == 'nt':
            import ctypes
            from ctypes import wintypes
            k = ctypes.windll.kernel32
            k.GetCurrentProcess.restype = wintypes.HANDLE
            k.SetPriorityClass.argtypes = [wintypes.HANDLE, wintypes.DWORD]
            k.SetPriorityClass(k.GetCurrentProcess(), 0x4000)
        else:
            os.nice(19)
    except (OSError, AttributeError):
        pass


# ---------------------------------------------------------------------------------------------------------------------
# rsync filter rules. This mirrors rsync 3.x exclude.c (add_rule, rule_matches) and lib/wildmatch.c for the rule forms
# backup-include.txt uses; any other form is refused rather than guessed, because a wrong guess publishes or drops files.

class FilterError(Exception):
    pass


def class_to_regex(p, i):
    """Translate the bracket expression starting at p[i] == '[' the way wildmatch's dowild() reads it.
    Returns (regex, index after the closing ']')."""
    n = len(p)
    i += 1
    if i >= n:
        raise FilterError('unterminated character class')
    negated = p[i] in '!^'
    if negated:
        i += 1
    items = []
    prev = None
    first = True
    while True:
        if i >= n:
            raise FilterError('unterminated character class')
        c = p[i]
        if c == ']' and not first:
            break
        first = False
        if c == '\\':
            i += 1
            if i >= n:
                raise FilterError('unterminated character class')
            c = p[i]
            items.append((c, c))
            prev = c
        elif c == '-' and prev is not None and i + 1 < n and p[i + 1] != ']':
            i += 1
            hi = p[i]
            if hi == '\\':
                i += 1
                if i >= n:
                    raise FilterError('unterminated character class')
                hi = p[i]
            items.append((prev, hi))
            # dowild() zeroes prev_ch after a range, so "a-c-e" is a range, a literal '-', and 'e'.
            prev = None
        elif c == '[' and i + 1 < n and p[i + 1] == ':':
            raise FilterError('POSIX character classes ([:name:]) are not supported here')
        else:
            items.append((c, c))
            prev = c
        i += 1
    parts = ''.join(re.escape(a) if a == b else f'{re.escape(a)}-{re.escape(b)}' for a, b in items if a <= b)
    # A class never matches '/' in rsync, negated or not.
    if negated:
        return f'[^{parts}/]', i + 1
    return (f'(?!/)[{parts}]' if parts else '(?!)'), i + 1


def wild_to_regex(p):
    """'*' stops at '/', two or more '*' cross it, '?' is any character but '/', '\\' escapes the next character."""
    rx = []
    i, n = 0, len(p)
    while i < n:
        c = p[i]
        if c == '\\':
            if i + 1 >= n:
                raise FilterError('pattern ends with a backslash')
            rx.append(re.escape(p[i + 1]))
            i += 2
        elif c == '*':
            j = i
            while j < n and p[j] == '*':
                j += 1
            rx.append('.*' if j - i > 1 else '[^/]*')
            i = j
        elif c == '?':
            rx.append('[^/]')
            i += 1
        elif c == '[':
            cls, i = class_to_regex(p, i)
            rx.append(cls)
        else:
            rx.append(re.escape(c))
            i += 1
    return ''.join(rx)


def trailing_elements(text, count):
    """wildmatch.c trailing_N_elements(): the last `count` '/'-separated elements, or None if there are fewer."""
    i = len(text)
    while True:
        i = text.rfind('/', 0, i)
        if i < 0:
            return text if count == 1 else None
        count -= 1
        if count == 0:
            return text[i + 1:]


class Rule:
    def __init__(self, include, pattern, origin):
        self.include = include
        self.origin = origin
        self.text = pattern
        self.directory = len(pattern) > 1 and pattern.endswith('/')
        if self.directory:
            pattern = pattern[:-1]
        self.slash_cnt = pattern.count('/')
        self.anchored = pattern.startswith('/')
        self.body = pattern[1:] if self.anchored else pattern
        self.wild = any(c in pattern for c in '*?[')
        star2 = pattern.find('**') if self.wild else -1
        self.wild2 = star2 >= 0
        self.wild2_prefix = star2 == 0
        self.wild3_suffix = self.wild2 and pattern.endswith('***')
        try:
            self.regex = re.compile(wild_to_regex(self.body), re.S) if self.wild else None
        except FilterError as e:
            raise FilterError(f'{origin}: {e}') from None
        if not self.anchored and self.slash_cnt and not self.wild2:
            # An unanchored pattern with an inner '/' and no '**' must match the last slash_cnt+1 elements.
            self.elements = self.slash_cnt + 1
        elif not self.anchored and not self.wild2_prefix and self.wild2:
            # An unanchored '**' pattern may start after any '/'.
            self.elements = -1
        else:
            self.elements = 0

    def matches(self, path, is_dir):
        """rule_matches() for a path relative to the transfer root ('a/b/c', no leading '/')."""
        if not self.slash_cnt and not self.wild2:
            path = path.rpartition('/')[2]
            text = path
        elif self.wild2_prefix:
            text = '/' + path
        else:
            text = path
        if is_dir:
            # "dir/***" also matches the directory itself: rsync appends '/' to a directory's name for it.
            if self.wild3_suffix:
                text += '/'
        elif self.directory:
            return False
        if not self.wild:
            if self.anchored:
                return path == self.body
            return path == self.body or path.endswith('/' + self.body)
        if self.elements > 0:
            text = trailing_elements(text, self.elements)
            return text is not None and self.regex.fullmatch(text) is not None
        if self.regex.fullmatch(text):
            return True
        if self.elements < 0:
            i = text.find('/')
            while i >= 0:
                if self.regex.fullmatch(text, i + 1):
                    return True
                i = text.find('/', i + 1)
        return False


def load_filter(path):
    rules = []
    text = path.read_bytes().decode('utf-8')
    # rsync ends a filter-file line at '\n' or at '\r'; lines starting with '#' or ';' are comments.
    for n, line in enumerate(re.split(r'\r\n|\r|\n', text), 1):
        if not line or line[0] in '#;':
            continue
        origin = f'{path.name}:{n}'
        if line[:2] not in ('+ ', '- ', '+_', '-_'):
            raise FilterError(f'{origin}: unsupported rule form {line!r} (only "+ " and "- " rules are evaluated)')
        pattern = line[2:]
        # rsync would keep trailing blanks as part of the pattern; that is almost certainly not what was meant.
        if not pattern or pattern != pattern.strip():
            raise FilterError(f'{origin}: empty pattern or leading/trailing blanks in {line!r}')
        rules.append(Rule(line[0] == '+', pattern, origin))
    if not rules:
        raise FilterError(f'{path}: no rules')
    return rules


def excluded(rules, path, is_dir):
    # First matching rule wins; a path no rule matches is included.
    for r in rules:
        if r.matches(path, is_dir):
            return not r.include
    return False


def select(rules, root, max_size=None):
    """The files rsync -a [--max-size] would send from root/ with these rules, as sorted relative paths. An excluded
    directory is never descended into, which is how the final '- *' keeps every unlisted directory out.
    Returns (files, too_big, other) where other holds included symlinks, junctions and special files."""
    files, too_big, other = [], [], []
    stack = ['']
    while stack:
        rel_dir = stack.pop()
        with os.scandir(root / rel_dir) as it:
            entries = sorted(it, key=lambda e: e.name)
        for e in entries:
            rel = f'{rel_dir}/{e.name}' if rel_dir else e.name
            link = e.is_symlink() or e.is_junction()
            is_dir = not link and e.is_dir(follow_symlinks=False)
            if excluded(rules, rel, is_dir):
                continue
            if is_dir:
                stack.append(rel)
            elif link or not e.is_file(follow_symlinks=False):
                other.append(rel)
            elif max_size is not None and e.stat(follow_symlinks=False).st_size > max_size:
                too_big.append(rel)
            else:
                files.append(rel)
    return sorted(files), sorted(too_big), sorted(other)


# ---------------------------------------------------------------------------------------------------------------------

def ensure_worktree():
    if not WT.exists():
        say(f'creating the worktree {WT.as_posix()} of the local branch {BRANCH}')
        git('worktree', 'add', WT.as_posix(), BRANCH, cwd=AB)
    top = git('rev-parse', '--show-toplevel', check=False)
    if top.returncode != 0 or Path(out(top)).resolve() != WT.resolve():
        die(f'{WT} exists but is not a git worktree of its own; move it aside and re-run')
    common = Path(out(git('rev-parse', '--path-format=absolute', '--git-common-dir'))).resolve()
    if common != (AB / '.git').resolve():
        die(f'{WT} belongs to {common}, not to {AB}')
    head = out(git('symbolic-ref', '--short', '-q', 'HEAD', check=False))
    if head != BRANCH:
        die(f'{WT} is on {head or "a detached HEAD"}, not on {BRANCH}; refusing to run')


def ancestor_guard(strict):
    """Never build on top of a history that GitHub has moved past: GitHub's head must be an ancestor of ours. A dry run
    or a plain copy publishes nothing, so there the guard only warns."""
    def problem(msg):
        if strict:
            die(msg)
        warn(msg + '\n  (not committing in this mode, so continuing; --commit would stop here)')

    p = subprocess.run(['git', 'ls-remote', AB_URL, f'refs/heads/{BRANCH}'], capture_output=True)
    if p.returncode != 0:
        problem(f"could not read GitHub's {BRANCH} head: {p.stderr.decode('utf-8', 'replace').strip()}")
        return
    fields = out(p).split()
    if not fields:
        say(f'GitHub has no {BRANCH} branch yet; nothing to guard against')
        return
    remote = fields[0]
    if git('cat-file', '-e', f'{remote}^{{commit}}', check=False).returncode != 0:
        problem(f"GitHub's {BRANCH} head {remote[:12]} is not in the local repository: fetch it "
                f'(git -C {AB.as_posix()} fetch origin {BRANCH}) and bring it into {BRANCH} first; never force-push')
        return
    rc = git('merge-base', '--is-ancestor', remote, 'HEAD', check=False).returncode
    if rc == 1:
        problem(f"GitHub's {BRANCH} head {remote[:12]} is not an ancestor of the worktree HEAD "
                f"{out(git('rev-parse', '--short=12', 'HEAD'))}: merge or fast-forward it first; never force-push")
    elif rc != 0:
        problem(f'git merge-base --is-ancestor failed with exit code {rc}')
    else:
        say(f"GitHub's {BRANCH} head {remote[:12]} is an ancestor of the worktree HEAD")


def redact(data):
    for rx, repl in REDACTIONS:
        data = rx.sub(repl, data)
    return data


class Copier:
    def __init__(self):
        self.new = self.updated = self.same = 0

    def put(self, data, rel):
        """Write only when the bytes differ, so unchanged files keep their stat data and git rehashes nothing."""
        if rel in REDACT_TARGETS:
            data = redact(data)
        dst = WT / rel
        try:
            st = dst.stat()
        except FileNotFoundError:
            st = None
        if st is not None:
            if not stat.S_ISREG(st.st_mode):
                die(f'{dst} exists and is not a regular file')
            if st.st_size == len(data) and dst.read_bytes() == data:
                self.same += 1
                return
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(data)
        if st is None:
            self.new += 1
        else:
            self.updated += 1

    def copy(self, src, rel):
        self.put(src.read_bytes(), rel)


def leftover_passwords():
    """The shell version's grep -rIn over the whole copy (binary files and .git skipped), reported as path:line only."""
    hits = []
    for root, dirs, names in os.walk(WT):
        dirs[:] = sorted(d for d in dirs if d != '.git')
        for name in sorted(names):
            if name in LEFTOVER_SKIP_NAMES:
                continue
            p = Path(root, name)
            data = p.read_bytes()
            if b'\0' in data or not LEFTOVER.search(data):
                continue
            rel = p.relative_to(WT).as_posix()
            for n, line in enumerate(data.split(b'\n'), 1):
                if LEFTOVER.search(line) and b'user:pass@' not in line:
                    hits.append(f'{rel}:{n}')
    return hits


def staged_changes():
    """(status, path) pairs of the staged change; renames are split into a delete and an add so no path escapes."""
    raw = git('diff', '--cached', '--name-status', '-z', '--no-renames').stdout.split(b'\0')
    pairs = []
    for i in range(0, len(raw) - 1, 2):
        pairs.append((raw[i].decode(), raw[i + 1].decode('utf-8', 'surrogateescape')))
    return pairs


def load_terms():
    if not TERMS_FILE.is_file():
        die(f'{TERMS_FILE} is missing: the privacy guard cannot run, so nothing is staged for publishing')
    terms = []
    for line in TERMS_FILE.read_text('utf-8').splitlines():
        t = line.strip()
        if t and not t.startswith('#'):
            terms.append(t.casefold())
    if not terms:
        die(f'{TERMS_FILE} lists no terms: the privacy guard would check nothing, so refusing to continue')
    return terms


def masked(path, terms):
    """A path safe to print: a never-publish term inside a file name must not reach the terminal either."""
    shown = path
    for t in terms:
        shown = re.sub(re.escape(t), '***', shown, flags=re.I)
    return shown if not any(t in shown.casefold() for t in terms) else '<path withheld: it holds a never-publish term>'


def staged_blobs(paths):
    """Yield (path, bytes) of the staged (index) version of each path."""
    index = {}
    for entry in git('ls-files', '-s', '-z').stdout.split(b'\0'):
        if entry:
            meta, _, name = entry.partition(b'\t')
            index[name.decode('utf-8', 'surrogateescape')] = meta.split()[1]
    with subprocess.Popen([*GIT, '-C', str(WT), 'cat-file', '--batch'],
                          stdin=subprocess.PIPE, stdout=subprocess.PIPE) as proc:
        for path in paths:
            proc.stdin.write(index[path] + b'\n')
            proc.stdin.flush()
            header = proc.stdout.readline().split()
            if len(header) != 3:
                die(f'git cat-file could not read the staged {path}')
            data = proc.stdout.read(int(header[2]))
            proc.stdout.read(1)
            yield path, data
        proc.stdin.close()


def forbidden_path(path):
    p = path.lower()
    return (p.startswith('.overhaul/perf/wp11/') or p.startswith('.overhaul/logs/wp11/')
            or p == '.overhaul/audit/periflow-learnings.md'
            or any(fnmatch.fnmatchcase(part, 'periflow_*') for part in p.split('/'))
            or 'scratch/pf-diff' in p or 'scratch/vitfleet' in p)


def run_guards(changes, message, terms):
    """Every check on what `git add --all --force` staged. Values and terms are never printed: only paths (with any
    term masked), line numbers and counts."""
    problems = []
    added = [p for s, p in changes if s[0] in 'AMT']

    def verdict(name, detail):
        say(f'guard {name}: ' + ('FAILED' if detail else 'ok'))
        if detail:
            problems.append(f'{name}:\n' + '\n'.join(detail))

    verdict('secret-like file names', [f'  {masked(p, terms)}' for p in added if SECRET_NAMES.search(p)])

    diff = git('diff', '--cached', '-U0', '--no-renames', '--no-color', '--no-ext-diff', '--no-textconv').stdout
    per_file, current = {}, '?'
    for line in diff.split(b'\n'):
        if line.startswith(b'+++ '):
            current = line[6:].decode('utf-8', 'surrogateescape') if line.startswith(b'+++ b/') else '?'
        if line.startswith(b'+') and b'PLACEHOLDER' not in line and SECRET_TEXT.search(line):
            per_file[current] = per_file.get(current, 0) + 1
    verdict('secret-like strings on added lines (values not printed)',
            [f'  {masked(p, terms)}: {c} line(s)' for p, c in sorted(per_file.items())])

    # The whole index, not only the change: the commit publishes the full tree, and HEAD is already clean of these.
    index = [n.decode('utf-8', 'surrogateescape') for n in git('ls-files', '-z').stdout.split(b'\0') if n]
    verdict('private PeriFlow / VitFleet paths in the commit tree',
            [f'  {masked(p, terms)}' for p in index if forbidden_path(p)])

    hits, binary = [], 0
    for path in added:
        if any(t in path.casefold() for t in terms):
            hits.append(f'  {masked(path, terms)} (file name)')
    for path, data in staged_blobs(added):
        # git's own text/binary test: a NUL in the first 8000 bytes.
        if b'\0' in data[:8000]:
            binary += 1
            continue
        text = data.decode('utf-8', 'replace')
        if not any(t in text.casefold() for t in terms):
            continue
        for n, line in enumerate(text.split('\n'), 1):
            folded = line.casefold()
            if any(t in folded for t in terms):
                hits.append(f'  {masked(path, terms)}:{n}')
    if any(t in message.casefold() for t in terms):
        hits.append('  the commit message')
    verdict(f'never-publish terms of {TERMS_FILE.name} (terms not printed)', hits)

    say(f'guards covered {len(added)} added/modified file(s) ({binary} binary, skipped by the text checks) and '
        f'{len(index)} path(s) in the index')
    return problems


def summarise(changes):
    groups = {}
    for status, path in changes:
        parts = path.split('/')
        if parts[0] == '.overhaul':
            key = '.overhaul/' + parts[1] if len(parts) > 2 else '.overhaul (files)'
        else:
            key = parts[0] + '/' if len(parts) > 1 else '(branch root)'
        g = groups.setdefault(key, {'A': 0, 'M': 0, 'other': 0})
        g[status[0] if status[0] in 'AM' else 'other'] += 1
    a = sum(g['A'] for g in groups.values())
    m = sum(g['M'] for g in groups.values())
    o = sum(g['other'] for g in groups.values())
    say(f'staged change: {a} added, {m} modified, {o} other (deleted / type changed)')
    width = max(len(k) for k in groups)
    print(f'  {"folder":<{width}}  added  modified  other')
    for key in sorted(groups):
        g = groups[key]
        print(f'  {key:<{width}}  {g["A"]:>5}  {g["M"]:>8}  {g["other"]:>5}')


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[0],
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    mode = ap.add_mutually_exclusive_group()
    mode.add_argument('--dry-run', action='store_true',
                      help='copy and redact, stage, run the guards, print a summary, then unstage (git reset -q)')
    mode.add_argument('--commit', action='store_true', help='copy, redact, stage, run the guards and commit')
    ap.add_argument('--push', action='store_true',
                    help=f'with --commit: push HEAD to {BRANCH} on origin (fast-forward only)')
    ap.add_argument('--message', default=f'Back up the overhaul workspace ({datetime.date.today().isoformat()})',
                    help='the commit subject (default: "Back up the overhaul workspace (<date>)")')
    args = ap.parse_args()
    if args.push and not args.commit:
        ap.error('--push needs --commit')
    if not args.message.strip():
        ap.error('--message is empty')

    for f in (FILTER_FILE, LIVE_TOOLS_CLAUDE, H / 'CLAUDE.md', H / 'HANDOVER.md'):
        if not f.is_file():
            die(f'{f} not found (is the tools root {TOOLS} right?)')
    if not (AB / '.git').is_dir():
        die(f'{AB} is not a git clone')
    if not MEM_SRC.is_dir():
        die(f'memory folder {MEM_SRC} not found')
    terms = load_terms() if (args.dry_run or args.commit) else None
    try:
        rules = load_filter(FILTER_FILE)
    except FilterError as e:
        die(f'filter: {e}')
    if not excluded(rules, 'wt', True):
        die(f'{FILTER_FILE.name} no longer excludes /wt/: the worktree would be copied into itself')

    lower_priority()
    ensure_worktree()
    ancestor_guard(strict=args.commit)

    # The tools-root CLAUDE.md lives outside .overhaul; its copy in handover/ is what the branch carries.
    live = LIVE_TOOLS_CLAUDE.read_bytes()
    mirror = H / 'tools-CLAUDE.md'
    if not mirror.is_file() or mirror.read_bytes() != live:
        mirror.write_bytes(live)
        say(f'refreshed {mirror.relative_to(TOOLS).as_posix()} from {LIVE_TOOLS_CLAUDE.as_posix()}')

    cp = Copier()
    files, too_big, other = select(rules, OV, MAX_SIZE)
    for rel in files:
        cp.copy(OV / rel, f'.overhaul/{rel}')
    say(f'.overhaul: the filter selects {len(files)} file(s); {len(too_big)} over 50 MiB skipped'
        + (f': {", ".join(too_big)}' if too_big else ''))
    if other:
        warn('symlinks, junctions or special files are not copied: ' + ', '.join(other))

    handover_rules = [Rule(False, '/local-only/', 'second pass'), Rule(False, '/claude-memory/', 'second pass')]
    h_files, _, h_other = select(handover_rules, H)
    for rel in h_files:
        cp.copy(H / rel, f'.overhaul/handover/{rel}')
    if h_other:
        warn('symlinks, junctions or special files in handover/ are not copied: ' + ', '.join(h_other))
    for name in ('CLAUDE.md', 'HANDOVER.md', 'tools-CLAUDE.md'):
        cp.copy(H / name, name)
    if (H / 'BRANCH-README.md').is_file():
        cp.copy(H / 'BRANCH-README.md', 'README.md')
    # private-*.md memories hold the user's other work (an employer's project, Oct 20): they never leave this machine.
    memory = sorted(e.name for e in os.scandir(MEM_SRC)
                    if e.is_file(follow_symlinks=False) and fnmatch.fnmatchcase(e.name, '*.md')
                    and not e.name.startswith('private-'))
    for name in memory:
        cp.copy(MEM_SRC / name, f'claude-memory/{name}')
    # Covers the worktree's own checked-out copies too, whether or not this run rewrote them.
    for rel in REDACT_TARGETS:
        f = WT / rel
        if f.is_file():
            data = f.read_bytes()
            clean = redact(data)
            if clean != data:
                f.write_bytes(clean)
    say(f'copied into {WT.as_posix()}: {cp.new} new, {cp.updated} updated, {cp.same} unchanged '
        f'(handover second pass {len(h_files)} file(s), {len(memory)} memory file(s))')

    leftover = leftover_passwords()
    if leftover:
        die('a database password is left in the copy (values not printed):\n'
            + '\n'.join(f'  {h}' for h in leftover) + '\nredact it (REDACTIONS) and re-run')

    if not (args.dry_run or args.commit):
        say(f'copied and redacted into {WT.as_posix()} (no --dry-run or --commit: nothing staged)')
        return

    add = git('add', '--all', '--force')
    notes = [ln for ln in add.stderr.decode('utf-8', 'replace').splitlines() if ln.strip()]
    if notes:
        say(f'git add printed {len(notes)} line(s), e.g.: {notes[0]}')
    changes = staged_changes()
    message = f'{args.message.rstrip()}\n\n{TRAILER}\n'
    if not changes:
        say('nothing changed; nothing to commit')
    else:
        summarise(changes)
        problems = run_guards(changes, message, terms)
        if problems:
            git('reset', '-q')
            for p in problems:
                print(p, file=sys.stderr)
            die(f'not committed and unstaged: review the hits (git -C {WT.as_posix()} diff), exclude or redact them, '
                'and re-run')
        if args.dry_run:
            git('reset', '-q')
            say('dry run: guards passed; unstaged again (git reset -q); the copy stays in the worktree')
            return
        git('commit', '-q', '-F', '-', stdin=message.encode('utf-8'))
        stat_line = out(git('show', '--stat', '--format=', 'HEAD')).splitlines()
        say(f"committed {out(git('rev-parse', '--short', 'HEAD'))}: {stat_line[-1].strip() if stat_line else ''}")

    if args.push:
        url = out(git('remote', 'get-url', '--push', 'origin'))
        if url.rstrip('/') != AB_URL:
            die(f'origin pushes to {url}, not {AB_URL}; refusing to push')
        p = subprocess.run([*GIT, '-c', 'pack.threads=1', '-C', str(WT), 'push', 'origin',
                            f'HEAD:refs/heads/{BRANCH}'])
        if p.returncode != 0:
            die('push failed (it is never forced: if GitHub moved on, bring its commits in first)')
        remote = subprocess.run(['git', 'ls-remote', AB_URL, f'refs/heads/{BRANCH}'], capture_output=True)
        say(f'GitHub {BRANCH}: {out(remote)[:12]}; local: {out(git("rev-parse", "--short=12", "HEAD"))}')


if __name__ == '__main__':
    main()
