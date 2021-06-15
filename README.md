# Not Another Config Library

Yep, it's a config library. Sorry.

Basically it's "the handrolled config thing I write in every mod that needs a config, while growling to myself that i should probably make this into a library some day"

## Why use `nacl`?

* Lightweight.
* Depends only on Minecraft and its libraries. Not even Fabric.
* Config format is simple, user-friendly, and has comments.
* Annotation-powered POJO config, no "intermediate representation".
  * Yes, annotations; but it's flexible enough to handle any type you wanna throw at it.
* Assumes little about your mod.

## Why *not* use `nacl`?

* You won't find server -> client config syncing.
* Config values can't span multiple lines, or begin/end with whitespace.
* There is no "intermediate representation" for config files.

## Usage

### Get started

1. Make a class with a public zero-argument constructor. Stick a bunch of fields in it, and set them to their default values. This is your config class.
2. Make a `ConfigReader`. It's stateless.
3. Call `ConfigHolder#read`, pass the path you want the config file to live in.
4. That's it.

### `ConfigOperations`

Implementing `ConfigOperations` on your config POJO creates some callbacks:

(todo)

### `Codon`

`Codon`s are shitty versions of DataFixerUpper's `Codec`. No `DataResult`s here, just these two methods:

```java
interface Codon<T> {
	String write(Field targetField, T value);
	T parse(Field sourceField, String value);
}
```

(Perform data validation in here - that's why you're given access to the `Field` of your config object, so you can do things like inspect its annotation for "at least / at most" bounds.)

Codons are provided for:

* `String`
* all primitive numeric types (byte, short, int, long, float, double)
* `boolean`
* `Identifier`

Due to :sparkles: reflection magic :sparkles:, all of the following types can be used as well:

* Anything with a `Registry` defined in `Registry.class`: `Block`, `Item`, `SoundEvent`, you name it
* `List<T>` (comma-separated)
* `Set<T>` (comma-separated)
* `Optional<T>` (empties get serialized as the empty string)

Known shortcomings:

* `List<List<T>>` and ilk don't demarcate the inner/outer collections
* `List<Optional<T>>` might not work? Idk
* I want to support `Map<K, V>` but I don't

### Annotation tour

* `@Skip` - skip this field. You may also declare the field `transient`, `static`, or `final`.
* `@BlankLine` - add (one or more) extra blank lines to the config file, if you need more visual separation.
* `@Section` - Prints a large comment before this field, marking off a section of the config file. (Config sections are *not* separated into separate classes.)
* `@Comment` - Add a multiline comment. (Don't use `\n` characters in the string, pass an array of strings, one for each line.)
* `@Note` - Add a multiline comment prefixed with "Note: ".
* `@Example` - Add (one or more) comments prefixed with "Example: ".
* `@AtLeast` and `@AtMost` - Place bounds on numeric values. (Produces a config comment documenting the bounds.)

## Mavens

I don't have this on any mavens unfortunately. I use `publishToMavenLocal` to depend on it, for now.

## License

LGPL 3 or later