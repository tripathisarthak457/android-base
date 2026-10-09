package store

import (
	"context"
	"log/slog"
	"sync"
	"sync/atomic"
	"time"
)

// Writer records batches from one goroutine, so no request ever waits on the database to be
// answered. The queue is bounded: when the database falls behind, new batches
// are dropped and counted rather than held in memory without limit.
type Writer struct {
	store *Store
	log   *slog.Logger
	jobs  chan job

	mu     sync.RWMutex
	closed bool

	stop     context.CancelFunc
	stopping context.Context
	finished chan struct{}

	written atomic.Int64
	dropped atomic.Int64
	failed  atomic.Int64
}

type job struct {
	batch *Batch
	done  chan struct{}
}

// WriterStats are the writer's counters since the process started.
type WriterStats struct {
	Queued  int   `json:"queued"`
	Written int64 `json:"written"`
	Dropped int64 `json:"dropped"`
	Failed  int64 `json:"failed"`
}

// NewWriter starts the writer goroutine.
func NewWriter(store *Store, capacity int, log *slog.Logger) *Writer {
	stopping, stop := context.WithCancel(context.Background())
	w := &Writer{
		store:    store,
		log:      log,
		jobs:     make(chan job, capacity),
		stop:     stop,
		stopping: stopping,
		finished: make(chan struct{}),
	}
	go w.run()
	return w
}

// Submit queues a batch and returns a channel closed once it is written, failed or dropped. It never
// blocks.
func (w *Writer) Submit(b *Batch) <-chan struct{} {
	done := make(chan struct{})
	w.mu.RLock()
	defer w.mu.RUnlock()
	if w.closed {
		w.dropped.Add(1)
		close(done)
		return done
	}
	select {
	case w.jobs <- job{batch: b, done: done}:
	default:
		w.dropped.Add(1)
		close(done)
	}
	return done
}

func (w *Writer) run() {
	defer close(w.finished)
	for j := range w.jobs {
		w.handle(j)
	}
}

func (w *Writer) handle(j job) {
	defer close(j.done)
	select {
	case <-w.store.ready:
	case <-w.stopping.Done():
		w.dropped.Add(1)
		return
	}
	ctx, cancel := context.WithTimeout(w.stopping, 5*time.Second)
	defer cancel()
	if err := w.store.write(ctx, j.batch); err != nil {
		w.failed.Add(1)
		w.log.Warn("recording telemetry", "error", err)
		return
	}
	w.written.Add(1)
}

// Close stops accepting batches and writes what is queued, giving up when ctx ends.
func (w *Writer) Close(ctx context.Context) {
	w.mu.Lock()
	if !w.closed {
		w.closed = true
		close(w.jobs)
	}
	w.mu.Unlock()

	select {
	case <-w.finished:
	case <-ctx.Done():
		w.stop()
		<-w.finished
	}
	w.stop()
}

func (w *Writer) Stats() WriterStats {
	return WriterStats{
		Queued:  len(w.jobs),
		Written: w.written.Load(),
		Dropped: w.dropped.Load(),
		Failed:  w.failed.Load(),
	}
}
