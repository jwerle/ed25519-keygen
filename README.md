ed25519-keygen
==============

Simple tool to generate ed25519 key pairs and write them to disk. Written in Kotlin (Native) and backed by libuv and libsodium

## Installation

```sh
$ npm install -g jwerle/ed25519-keygen
```

## Prerequisites

* [Kotlin/Native](https://github.com/JetBrains/kotlin-native) and the `konanc` command line program.

## Usage

```
usage: ed25519-keygen [-hV] [options]

options:
  -h, --help           Show this message
  -V, --version        Output program version
  -o, --out <path>     Output file path
  -s, --seed <seed>    Seed value or path to seed file
  -f, --force          Force overwrites of files
  --postfix <postfix>  A postfix for output file names. Set to 'false'
to disable' (default: _ed25519)

```

## Example

```sh
$ ed25519-keygen -o id
$ cat id_ed25519 id_ed25519.pub ## outputs secret and public key respectively
```

## License

MIT
