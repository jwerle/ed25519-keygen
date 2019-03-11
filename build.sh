#!/bin/bash

konanc main.kt $(konanc-config -lr .) -p program -o ed25519-keygen && \
  mv -f ed25519-keygen.kexe ed25519-keygen
