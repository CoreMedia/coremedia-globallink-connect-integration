# Disabled Facade

This facade mainly serves as example how to implement custom connection types.
The implementation just throws exceptions on every interaction with the facade.
The exception states: _GCC Service disabled._ It may be useful to enable this
connection type for maintenance windows at GCC.

It is considered safe to switch to this connection type, even if there are
currently translation workflows running. For them it is like a short-circuit
for the GCC service not being available right now. So, there is no danger of
an invalid state being stored in the workflow.

