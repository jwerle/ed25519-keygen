import kotlin.system.exitProcess

import kotlin.coroutines.*
import kotlinx.cinterop.*

import kargv.parse

import datkt.sodium.crypto_sign_seed_keypair
import datkt.sodium.crypto_sign_keypair

import datkt.sodium.crypto_sign_PUBLICKEYBYTES
import datkt.sodium.crypto_sign_SECRETKEYBYTES

data class KeyPair(
  val publicKey: ByteArray,
  val secretKey: ByteArray
)

data class Options(
  var help: Boolean = false,
  var version: Boolean = false,
  var force: Boolean = false,
  var out: String? = "ed25519", // file.pub file.priv
  var seed: String? = null,

  // rest args
  var `--`: Array<String?> = emptyArray()
)

data class State(
  var seed: ByteArray? = null,
  var publicKeyFile: String? = null,
  var secretKeyFile: String? = null,
  var keyPair: KeyPair? = null
)

val options = Options()
val state = State()

val VERSION = "1.0.1"
val USAGE = "usage: ed25519-keygen [-hV] [options]"
val OPTIONS = """
options:
  -h, --help         Show this message
  -V, --version      Output program version
  -o, --out <path>   Output file path
  -s, --seed <seed>  Seed value or path to seed file
  -f, --force        Force overwrites of files
"""

fun keyPair(seed: ByteArray? = null): KeyPair {
  if (null != seed && seed.size < 32) {
    throw Error("Seed cannot be less than 32 bytes.")
  }

  val publicKey = ByteArray(crypto_sign_PUBLICKEYBYTES.toInt())
  val secretKey = ByteArray(crypto_sign_SECRETKEYBYTES.toInt())

  publicKey.usePinned { publicKeyPointer ->
    secretKey.usePinned { secretKeyPointer ->
      if (null != seed) {
        seed.usePinned { seedPointer ->
          crypto_sign_seed_keypair(
              publicKeyPointer.addressOf(0) as CValuesRef<UByteVar>,
              secretKeyPointer.addressOf(0) as CValuesRef<UByteVar>,
              seedPointer.addressOf(0) as CValuesRef<UByteVar>)
        }
      } else {
        crypto_sign_keypair(
          publicKeyPointer.addressOf(0) as CValuesRef<UByteVar>,
          secretKeyPointer.addressOf(0) as CValuesRef<UByteVar>)
      }
    }
  }

  return KeyPair(publicKey, secretKey)
}

fun parseCommandLineArguments(argv: Array<String>) {
  try {
    kargv.parse(argv, options) { o, node ->
      val value = node.value
      when (node.name) {
        "h", "help" -> o.help = true
        "V", "version" -> o.version = true
        "f", "force" -> o.force = true
        "s", "seed" -> o.seed = node.value
        "o", "out" -> o.out = node.value

      else ->
        if (null != node.name) {
          o.`--` += node.value
        } else {
          throw Error("unknown option: ${node.source}")
        }
      }
    }
  } catch (err: Error) {
    println("error: ${err.message}")
    println(USAGE)
    exitProcess(1)
  }

  if (true == options.help) {
    println(USAGE)
    println(OPTIONS)
    exitProcess(0)
  }
}

suspend fun writeFile(path: String, buffer: ByteArray) {
  return suspendCoroutine { cont ->
    datkt.fs.writeFile(path, buffer) { err ->
      if (null != err) {
        throw err
      }

      cont.resume(Unit)
    }

    datkt.fs.loop.run()
  }
}

suspend fun readFile(path: String): ByteArray? {
  return suspendCoroutine { cont ->
    datkt.fs.readFile(path) { err, buf ->
      if (null != err) {
        throw err
      }

      cont.resume(buf)
    }

    datkt.fs.loop.run()
  }
}

suspend fun statFile(path: String): datkt.fs.Stats? {
  return suspendCoroutine { cont ->
    datkt.fs.stat(path) { err, stat ->
      if (null != err) {
        throw err
      }

      cont.resume(stat)
    }

    datkt.fs.loop.run()
  }
}

suspend fun accessFile(path: String) {
  return suspendCoroutine { cont ->
    datkt.fs.access(path) { err ->
      if (null != err) {
        cont.resumeWithException(err)
      } else {
        cont.resume(Unit)
      }
    }

    datkt.fs.loop.run()
  }
}

suspend fun start() {
  val publicKeyFile = "${options.out}_key.pub"
  val secretKeyFile = "${options.out}_key"
  val seed = options.seed

  if (null != seed) {
    try {
      accessFile(seed)
      state.seed = readFile(seed)
    } catch (err: Error) {
      state.seed = seed.toUtf8()
    }
  }

  val (publicKey, secretKey) = keyPair(state.seed)

  suspend fun save(path: String, key: ByteArray) {
    if (true == options.force) {
      val stat = statFile(path)
      if (null != stat && stat.isDirectory()) {
        throw Error("Cannot overwrite directory: ${path}")
      } else {
        writeFile(path, key)
      }
    } else {
      try {
        accessFile(path)
      } catch (err: Error) {
        writeFile(path, key)
        return
      }

      throw Error("Cannot overwrite existing file: ${path}")
    }
  }

  save(publicKeyFile, publicKey)
  save(secretKeyFile, secretKey)

  state.publicKeyFile = publicKeyFile
  state.secretKeyFile = secretKeyFile
  state.keyPair = KeyPair(publicKey, secretKey)
}

fun launch(block: suspend () -> Unit) {
  return block.startCoroutine(Continuation(EmptyCoroutineContext) { r ->
    r.onFailure { err -> println("error: ${err.message}") }
    r.onSuccess { }
  })
}

/**
 * Main program entry
 */
fun main(argv: Array<String>) {
  parseCommandLineArguments(argv)
  launch { start() }
}
