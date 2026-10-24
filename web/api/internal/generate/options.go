package generate

import "fmt"

// OptionLockedError refuses a spec that asks for a value of an option this deployment does not
// generate. Note is the catalogue's own sentence, written for the visitor.
type OptionLockedError struct {
	Field string
	Note  string
}

func (e *OptionLockedError) Error() string { return e.Note }

// CheckOptions holds a prechecked spec to the options the target declares: a value that is not true
// or false is refused like any other invalid field, and a locked option refuses every value but its
// own, counting a field the spec leaves out (or sets to null, which the generator reads the same
// way) as the option's default. It runs before the spec takes a generator slot. A target without
// options is left to its generator, which refuses a field it does not know.
func (t *TargetEntry) CheckOptions(spec Spec) error {
	for _, option := range t.Options {
		value := *option.Default
		if raw := spec.fields[option.Key]; raw != nil {
			given, ok := raw.(bool)
			if !ok {
				return &PrecheckError{Message: fmt.Sprintf("`%s` must be true or false.", option.Key), Field: option.Key}
			}
			value = given
		}
		if option.Locked != nil && value != *option.Locked.Value {
			return &OptionLockedError{Field: option.Key, Note: option.Locked.Note}
		}
	}
	return nil
}

// LockedValues are the values a spec must give for the target's locked options.
func (t *TargetEntry) LockedValues() map[string]bool {
	values := map[string]bool{}
	for _, option := range t.Options {
		if option.Locked != nil {
			values[option.Key] = *option.Locked.Value
		}
	}
	return values
}
