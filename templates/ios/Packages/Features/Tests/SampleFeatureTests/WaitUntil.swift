import Observation

/// Returns once `condition` holds, checking it again each time something it reads changes.
///
/// It waits on the store's own changes instead of polling the clock, so a slow machine cannot fail
/// a test; the suite's time limit ends a wait for a change that never comes.
@MainActor
func waitUntil(_ condition: () -> Bool) async {
    while !condition() {
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            withObservationTracking {
                _ = condition()
            } onChange: {
                continuation.resume()
            }
        }
    }
}
