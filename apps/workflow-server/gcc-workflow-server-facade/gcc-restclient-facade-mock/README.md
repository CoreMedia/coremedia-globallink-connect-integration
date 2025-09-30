# Mock Facade

The mock facade will simulate a translation service in that way, that it replaces
the target nodes (pre-filled with values from source nodes) in XLIFF with some
other characters. As a result you will see that upon translation something
happened to the target document, i.e., it received a content update.

It is recommended to remove this facade in production environments, as it is
considered harmful to switch from the default facade to mock facade and vice
versa, as it will produce inconsistent states for running workflows.

## Mock Facade Configuration

You can configure the behaviour of the Mock Facade via the GlobalLink settings
(see: [Configuring GlobalLink connection settings](../../../../README.md)).

Therefore, you can set the following parameters within the `mock` section
(as sub-struct of the GlobalLink settings):

* `stateChangeDelaySeconds` base (minimum) offset in seconds  (type:`Integer`)
* `stateChangeDelayOffsetPercentage` percentage offset to the base delay, which
  will either reduce or increase the delay (type:`Integer`)
* `scenario` (type:`String`) If you want to test specific scenarios you can set
  this parameter to:
  * `cancelation-not-found`: Simulates a submission not found error on
    cancelation.
  * `full-regular-approval-state-flow`: Simulates a submission that passes all
    regular states, including approval states. This enriches normal mocking,
    that skips several intermediate states for simplicity.
  * `gcc-outage-on-cancelation`: Simulates a permanent GCC connection outage on
    cancelation.
  * `gcc-outage-on-download`: Simulates a permanent GCC connection outage on
    XLIFF download.
  * `gcc-outage-on-upload`: Simulates a permanent GCC connection outage on XLIFF
    upload.
  * `submission-canceled-by-globallink`: Simulates a submission that got
    canceled via GCC backend.
  * `submission-error`: Simulates a submission that reaches the (unrecoverable)
    error state at GCC.
  * `submission-redelivered`: Simulates a typical redelivered scenario, with
    first corrupted XLIFF, then signalling redelivered state.
  * `translate-invalid-xliff`: Simulates a permanently corrupted XLIFF shipped
    by GCC.
  * `translate-string-too-long`: Simulates a translation with long strings
    returned to provoke a string-too-long failure. Works best, if strings to
    translate are from a string property with a max-length less than 2048.
