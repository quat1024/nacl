# Not Another Config Library

Yep, it's a config library. Sorry.

Basically it's "the handrolled config thing I write in every mod that needs a config, while growling to myself that i should probably make this into a library some day"

# Why use `nacl`?

* Lightweight.
* Depends only on Minecraft and its libraries. Doesn't even depend on Fabric.
* Config format is simple, familiar, user-friendly, and has comments.
* Annotation-powered POJO config that can handle any type you wanna throw at it.
* Assumes little about your mod.

# Why not use `nacl`?

* You won't find server -> client config syncing.
* Config values can't span multiple lines, or begin/end with whitespace. The parser kinda sucks.
* There is no "intermediate representation" for config files.
* I don't have a lot of experience writing APIs for others to consume! :)

# Usage

## Getting started

1. Make a class with a public zero-argument constructor. Add fields and initialize them to their default values. Static, final, `transient`, and `@Skip`-annotated fields are skipped.
2. Make a `new ConfigReader()`. Think of them like Google GSON objects: stateless, can be reused for many different config files.
3. Call `ConfigReader#read`. Pass `MyConfig.class`, and the `Path` you want the config file to live at.

All done. This returns an instance of your config class, configured according to the config file. (If no config file existed, the default config file is written.)

## Callbacks

If your config class implements `ConfigExt`, the following callbacks become available:

* `upgrade(HashMap<String, String> unknownKeys)` - Called after parsing the config file. These keys exist in the config file, but don't correspond to any fields in your config class. Handle them here.
* `validate()` - Called after `upgrade`. `nacl` provides some simple numeric validation (at least X, at most X). If you have more complex validation needs, do them in here. Throw a `ConfigParseException` on error.
* `finish()` - Called if your config file successfully parses. This is a good place to create Java objects derived from fields in the config file, if you need to do that.

## Annotations

Decorate your config class's fields with these.

| annotation           | what it do :eyes: |
| :------------------: | :---------------- |
| `@AtLeast` `@AtMost` | Place bounds on numeric values. Produces a config comment documenting the bounds, and validates that the bounds are respected. |
| `@BlankLine`         | Add extra blank lines to the config file. |
| `@Comment`           | Add a multiline comment. Pass an array of strings, one for each line. |
| `@Example`           | Add (one or more) comment lines prefixed with "Example: ". |
| `@Note`              | Add a multiline comment prefixed with "Note: ". |
| `@Section`           | Print a big header comment before this field. (Config sections are *not* separated into separate classes, like e.g. forge 1.12 annotation config.) |
| `@Skip`              | Skip this field. (You may also declare the field `transient`, `static`, or `final`.) |
| `@SkipDefault`       | Don't write a "default value" comment for this config field. (You can also specify a string, to write a different one.) |
| `@Use`               | Use this named codon to de/serialize this field, instead of trying to guess by reflecting the field type - see below. |

## `Codon`s

`Codon`s are shitty versions of DataFixerUpper's `Codec`. No `DataResult`s here, just these two methods:

```java
interface Codon<T> {
	String write(Field targetField, T value);
	T parse(Field sourceField, String value);
}
```

`write` is used to include the default value of your config as a comment, and `parse` is used to read it back from the config file. You're given access to the `Field` of your config object so you can do things like, inspect its annotation for "at least / at most" bounds. Throw an exception if validation fails.

There are three ways to specify which `Codon` will be used for a given field:

* Automatically. `nacl` will use reflection to guess the correct `Codon`. Works for many common types.
* Classy codon. Register a `Codon` for a particular `Class<?>`.
* Named codon. Register a `Codon` under a given name, then use it for particular fields.

### Automatic

Codons are provided for:

* `String`
* all primitive numeric types (`byte`, `short`, `int`, `long`, `float`, `double`)
	* these respect `@AtLeast` and `@AtMost` annotations
* `boolean`
* `Identifier`
* Anything with a `Registry` defined in `Registry.class`: `Block`, `Item`, `SoundEvent`, you name it
	* Note that you must read the config file *after* these things are registered.

The following *type functions* are available as well. Unfortunately these are hardcoded for now.

* `List<T>` - produces a comma-separated list
* `Set<T>` - same
* `T[]` - same
* `Optional<T>` - empties get serialized as the empty string

### Classy codon

Call `ConfigReader#registerClassyCodon`, give it a `Class<?>` and a `Codon<?>` capable of reading and writing that class. If you've ever used Google GSON and had to tell it how to serialize a particular type: it's like that.

Type functions are aware of these; if you define a classy codon for `Heehoo` you get `List<Heehoo>` for free, etc.

### Named codon

Two steps:

* Call `ConfigReader#registerNamedCodon`, with a string name and a codon.
* Decorate relevant fields in your config class with `@Use("that name")`.

# Shortcomings

* Unboxed arrays (int[], etc) make it blow up for some reason!!!! Lists work okay. Dunno why.
* `List<List<T>>` and ilk don't demarcate the inner/outer collections.
* `List<Optional<T>>` might not work? Idk.
* I want to support `Map<K, V>` but I don't right now.
* There's no way for `@Use` to target, say, the `T` in `List<T>`. There is `Codon#listOf()`, as consolation.
* Error messages are not very good. Just kinda "exceptions thrown all over" and I'd like to include line number and some more details. Kinda like the "crashreport section" thing

# Maven

I don't have this on any mavens unfortunately. I use `publishToMavenLocal` to depend on it, for now.

## Debugging the test mod

Idk, how are you actually supposed to do test mods in Loom?

* Run the `runTest` gradle task without a debugger attached. (Debugger doesn't hurt anything, makes it slower though.)
* Watch the log, intelliJ will interactively ask you to attach a debugger. Click the thingie.
* Uh if the game doesn't crash, pressing Stop in intellij just like, detaches minecraft instead. So youll have to close the game yourself

# License

LGPL 3 or later