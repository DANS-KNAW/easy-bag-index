easy-bag-index
==============
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-bag-index.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-bag-index)

Index a bag store

SYNOPSIS
--------

    easy-bag-index index [--force | -f] <item-id> # Retrieves the information from the cache or directly from the bag-store
    easy-bag-index run-service # Runs the program as a service

DESCRIPTION
-----------
Provides an interface to add and retrieve relational metadata of individual bags from the index.

ARGUMENTS
---------

    Options:

    -h, --help      Show help message
    -v, --version   Show version of this program

    Subcommand: index - Starts EASY bag index as a daemon that services HTTP requests
        -f, --force  Force the indexing without asking for confirmation
        -h, --help   Show help message

    trailing arguments:
           item-id (required)

    ---

    Subcommand: run-service - Starts EASY bag index as a daemon that services HTTP requests
         -h, --help   Show help message

EXAMPLES
--------

```jshelllanguage
easy-bag-index index 40594b6d-8378-4260-b96b-13b57beadf7c
```

```json
{
  "result":{
    "bag-info":{
      "bag-id":"40594b6d-8378-4260-b96b-13b57beadf7c",
      "base-id":"40594b6d-8378-4260-b96b-13b57beadf7c",
      "created":"2015-05-19T00:00:00.000+02:00",
      "doi":"10.5072/dans-x6f-kf66"
    }
  }
}

```


INSTALLATION AND CONFIGURATION
------------------------------
Currently this project is build only as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/easy-bag-index` and the configuration files to `/etc/opt/dans.knaw.nl/easy-bag-index`.

To install the module on systems that do not support RPM, you can copy and unarchive the tarball to the target host.
You will have to take care of placing the files in the correct locations for your system yourself. For instructions
on building the tarball, see next section.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
* RPM

Steps:

        git clone https://github.com/DANS-KNAW/easy-bag-index.git
        cd easy-bag-index
        mvn install

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM
packaging will be activated. If `rpm` is available, but at a different path, then activate it by using
Maven's `-P` switch: `mvn -Pprm install`.

Alternatively, to build the tarball execute:

        mvn clean install assembly:single
