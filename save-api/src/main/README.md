# Save Cloud API Library

Current module provides the library for usage of Save Cloud API.

It provides the client from under the hood, which requires just a tiny configuration
for running the process of execution submission.

The four main characteristics are necessary for execution:

* **Configuration for web client** \
  Basically it's just the Save Cloud backend address
* **Configuration for evaluated tool** \
  Current configuration should contain general information about evaluated tool

* **Authorization data** \
  Credentials for authorization in Save Cloud system

* **Execution type** Git or standard


  Detailed description about these configurations could be found in
  [Backend-API.md](/../../../save-backend/Backend-API.md)