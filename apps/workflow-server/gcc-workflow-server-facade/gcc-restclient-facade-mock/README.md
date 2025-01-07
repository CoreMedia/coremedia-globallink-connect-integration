# Mock Facade

The mock facade will simulate a translation service in that way, that it replaces
the target nodes (pre-filled with values from source nodes) in XLIFF with some
other characters. As a result you will see that upon translation something
happened to the target document, i.e., it received a content update.

It is recommended to remove this facade in production environments, as it is
considered harmful to switch from the default facade to mock facade and vice
versa, as it will produce inconsistent states for running workflows.

## Control Task State Switching

This facade allows to control the task-state switching behavior by workflow
subject. If the subject does not contain any task-state switching information
it defaults to tasks switching automatically to _Completed_ state after a
certain time.

To use the task-state switching the subject must not contain anything despite
the task-state switching information. The structure of the subject must be
as follows (with some user-input convenience):

```text
states:<taskStateId>[, ...]
```

The default behavior could be written as:

```text
states:completed
```

Other examples:

```text
states:other,cancelled
```

For a complete list of supported task states see class `com.coremedia.labs.translation.gcc.facade.mock.TaskState`.
It also contains more information on state parsing.

Note, that some task states might not be useful to put into the list, as they
will be reached by normal workflow processes. You may of course still enforce
a switch to "Delivered" state after some time, if you want to simulate unexpected
states at GCC backend.

## Mock Facade Configuration

You can configure the behaviour of the Mock Facade via the GlobalLink settings (see: [Configuring GlobalLink connection settings](../../../../README.md)).
Therefore, you can set the following parameters: 
* `mockDelaySeconds` base (minimum) offset in seconds  (type:`Integer`)
* `mockDelayOffsetPercentage` percentage offset to the base delay, which will either reduce or increase the delay (type:`Integer`)
* `mockError` (type:`String`) If you want to test the Error Handling you can set this parameter to: 
  * `download_xliff` for an xliff import error
  * `download_communication` for a download communication error
  * `upload_communication` for an upload communication error
  * `cancel_communication` for a cancellation communication error
  * `cancel_result` for a failed or rejected cancellation
