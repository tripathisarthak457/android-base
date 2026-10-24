/** A choice a project without UI components does not make, with the reason, where the choice would be. */
export function HiddenChoice({ title, reason }: { title: string; reason: string }) {
  return (
    <div className="rounded-xl border border-dashed border-ink-600 p-4">
      <p className="text-sm font-medium text-ink-200">{title}</p>
      <p className="mt-1 text-xs leading-relaxed text-ink-400">Not asked without UI components. {reason}</p>
    </div>
  );
}
