package store

import (
	"context"
	"testing"
	"time"
)

func TestAFullQueueDropsTheBatchAndCountsIt(t *testing.T) {
	// Never connected: the writer takes the first batch and waits on it for the database, so
	// everything after it queues.
	s, err := New("postgres://wp09@127.0.0.1:1/none", "salt", quiet)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(s.Close)
	const capacity = 3
	w := NewWriter(s, capacity, quiet)

	waiting := w.Submit(&Batch{})
	for deadline := time.Now().Add(5 * time.Second); len(w.jobs) > 0; {
		if time.Now().After(deadline) {
			t.Fatal("the writer never took the first batch")
		}
		time.Sleep(time.Millisecond)
	}
	queued := make([]<-chan struct{}, capacity)
	for i := range queued {
		queued[i] = w.Submit(&Batch{})
	}

	select {
	case <-w.Submit(&Batch{}):
	default:
		t.Fatal("a batch that found the queue full was kept waiting")
	}
	if stats := w.Stats(); stats.Dropped != 1 || stats.Queued != capacity || stats.Written != 0 {
		t.Fatalf("stats %+v", stats)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 50*time.Millisecond)
	defer cancel()
	w.Close(ctx)
	for _, done := range append(queued, waiting) {
		select {
		case <-done:
		default:
			t.Fatal("Close returned with a batch still pending")
		}
	}
}
