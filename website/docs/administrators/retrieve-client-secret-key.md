---
sidebar_position: 2
description: How to get the client secret key via REST API.
---

# Retrieve Client Secret Key

If you did not receive a client secret key as part of your _Client Onboarding_
you can query it via REST API `/api/v3/connectors` which, provides a response
similar to this:

```json title="Example response /api/v3/connectors"
{
    "status": 200,
    "message": "Connector details",
    "response_data": [
        {
            "connector_key": "8dccce941d37cc697f41724d50c487d2",
            "connector_name": "Connector 1"
        },
        {
            "connector_key": "c89bc6f7fdbba90ed4a98d5d8d8a2662",
            "connector_name": "Connector 2"
        }
    ]
}
```
