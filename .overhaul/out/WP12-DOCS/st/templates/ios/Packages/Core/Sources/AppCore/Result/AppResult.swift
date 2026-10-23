/// The outcome of anything that can fail for a reason a screen has to explain.
///
/// Repositories return it rather than throwing, so a store's `switch` lists every outcome and an
/// unhandled error cannot escape into a view.
public typealias AppResult<Value> = Result<Value, AppError>
