## Contributing In General
Our project warmly welcomes contributions from users and partners. 
Please feel free to [open an issue](https://github.com/instana/otel-database-dc/issues) when you find something need to be improved.

To contribute code or documentation, please submit a [pull request](https://github.com/instana/otel-database-dc/pulls).


### Proposing new features

If you would like to implement a new feature, please [raise an issue](https://github.com/instana/otel-database-dc/issues)
before sending a pull request so the feature can be discussed. This is to avoid
you wasting your valuable time working on a feature that the project developers
are not interested in accepting into the code base.

### Fixing bugs

If you would like to fix a bug, please [raise an issue](https://github.com/instana/otel-database-dc/issues) before sending a
pull request so it can be tracked.

### Merge approval

The project maintainers use LGTM (Looks Good To Me) in comments on the code
review to indicate acceptance.

## Legal

Each source file must include a license header for the MIT
License. Using the SPDX format is the simplest approach.
e.g.

```
/*
Copyright <holder> All Rights Reserved.

SPDX-License-Identifier: MIT
*/
```

We have tried to make it as easy as possible to make contributions. This
applies to how we handle the legal aspects of contribution. We use the
same approach - the [Developer's Certificate of Origin 1.1 (DCO)](https://github.com/hyperledger/fabric/blob/master/docs/source/DCO1.1.txt) - that the Linux® Kernel [community](https://elinux.org/Developer_Certificate_Of_Origin)
uses to manage code contributions.

We simply ask that when submitting a patch for review, the developer
must include a sign-off statement in the commit message.

Here is an example Signed-off-by line, which indicates that the
submitter accepts the DCO:

```
Signed-off-by: John Doe <john.doe@example.com>
```

You can include this automatically when you commit a change to your
local git repository using the following command:

```
git commit -s
```

## Create a new data collector

Please refer to "[How to create a new data collector](docs/developer/new-db.md)".