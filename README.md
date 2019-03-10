ed25519-keygen
==============

Simple tool to generate ed25519 key pairs and write them to disk. Written in Kotlin (Native) and backed by libuv and libsodium

## Installation

```sh
$ npm install -g ed25519-keygen
```

## Usage

```
usage: ed25519-keygen [-hV] [options]

options:
  -h, --help         Show this message
  -V, --version      Output program version
  -o, --out <path>   Output file path
  -s, --seed <seed>  Seed value or path to seed file
  -f, --force        Force overwrites of files
```

## License

MIT
