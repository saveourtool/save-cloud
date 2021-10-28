![Build and test](https://github.com/cqfn/save-cloud/workflows/Build%20and%20test/badge.svg)
[![License](https://img.shields.io/github/license/cqfn/save-cloud)](https://github.com/cqfn/save-cloud/blob/master/LICENSE)
[![codecov](https://codecov.io/gh/cqfn/save-cloud/branch/master/graph/badge.svg)](https://codecov.io/gh/cqfn/save-cloud)

## What is SAVE?
[SAVE](https://github.com/cqfn/save) - is an eco-system for measuring, testing and certification of static code analyzers. Instead of writing your test framework SAVE will provide you a command line application with a
test sets for the language that you are developing analyzer for. It provides you also a service that can be used to determine the readiness of your tool. SAVE has a committee of static analysis experts
that regularly updates tests and discuss the best practices for particular programming languages.

## What is SAVE Cloud?
SAVE Cloud is a service for executing tests using the SAVE tool. You can provide a link to a git repository with a project, configured to
run SAVE. These tests will then be executed server-side, providing you access to execution results, statistics and logs.

## How it looks like for a user?
![image](https://user-images.githubusercontent.com/58667063/138879509-39bfcf1d-aec7-405d-801b-15145217c0b0.png)
![image](https://user-images.githubusercontent.com/58667063/138879602-bc9836a8-bb93-4409-b01a-ef96907e4fd6.png)

## Build
To build the project and run all tests, execute `./gradlew build`. For more detailed instructions, including deployment instructions, see [save-deploy/README.md](save-deploy/README.md).

## Architecture and design
<img src="/save.svg" width="1024px"/>
