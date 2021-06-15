package agency.highlysuspect.libs.nacl.v1.types;

import agency.highlysuspect.libs.nacl.v1.ConfigParseException;
import agency.highlysuspect.libs.nacl.v1.annotation.AtLeast;
import agency.highlysuspect.libs.nacl.v1.annotation.AtMost;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * It's like the DFU Codec, but shittier.
 */
public interface Codon<T> {
	String write(Field targetField, T value);
	T parse(Field sourceField, String value);
	
	//Go straight to Java generic hell, do not pass Go, do not collect $200.
	@SuppressWarnings("unchecked")
	default String writeErased(Field targetField, Object value) {
		return write(targetField, (T) value);
	}
	
	Codon<String> STRING = Codon.of((targetField, value) -> value, (targetField, value) -> value);
	
	Codon<Byte> BYTE = number("byte", Byte::parseByte, AtLeast::byteValue, AtMost::byteValue);
	Codon<Short> SHORT = number("short", Short::parseShort, AtLeast::shortValue, AtMost::shortValue);
	Codon<Integer> INTEGER = number("integer", Integer::parseInt, AtLeast::intValue, AtMost::intValue);
	Codon<Long> LONG = number("long", Long::parseLong, AtLeast::longValue, AtMost::longValue);
	Codon<Float> FLOAT = number("float", Float::parseFloat, AtLeast::floatValue, AtMost::floatValue);
	Codon<Double> DOUBLE = number("double", Double::parseDouble, AtLeast::doubleValue, AtMost::doubleValue);
	
	Codon<Boolean> BOOLEAN = ofToString((sourceField, value) -> {
		if(value.equalsIgnoreCase("true")) return true;
		else if(value.equalsIgnoreCase("false")) return false;
		else throw new ConfigParseException("Cannot parse " + value + " as a bool (true / false)");
	});
	
	Codon<Identifier> IDENTIFIER = ofToString((sourceField, value) -> {
		try {
			return new Identifier(value);
		} catch (Exception e) {
			throw new ConfigParseException("Cannot parse " + value + " as an Identifier");
		}
	});
	
	/**
	 * A codon that serializes elements of this registry as their Identifier.
	 */
	static <T> Codon<T> registryEntry(Registry<T> registry) {
		return IDENTIFIER.dimap(id -> {
			if(registry.containsId(id)) return registry.get(id);
			else throw new ConfigParseException("Cannot find something named " + id + " in registry " + registry);
		}, registry::getId);
	}
	
	/**
	 * So you don't have to go writing giant anonymous classes all the time.
	 */
	static <T> Codon<T> of(BiFunction<Field, T, String> writer, BiFunction<Field, String, T> reader) {
		return new Codon<>() {
			@Override
			public String write(Field targetField, T value) {
				return writer.apply(targetField, value);
			}
			
			@Override
			public T parse(Field sourceField, String value) {
				return reader.apply(sourceField, value);
			}
		};
	}
	
	/**
	 * A codon that's only able to parse, and can't write a default config value.
	 */
	static <T> Codon<T> parseOnly(String error, BiFunction<Field, String, T> parser) {
		return Codon.of((sourceField, value) -> { throw new ConfigParseException(error + " is a parse-only codon"); }, parser);
	}
	
	/**
	 * A codon that uses toString to serialize the value.
	 */
	static <T> Codon<T> ofToString(BiFunction<Field, String, T> parser) {
		return Codon.of((sourceField, value) -> value.toString(), parser);
	}
	
	/**
	 * Transforms this Codon&lt;T&gt; into a Codon&lt;U&gt;.
	 * @param into Function to turn a T into a U.
	 * @param from Function to turn a U back into a T.
	 */
	default <U> Codon<U> dimap(Function<T, U> into, Function<U, T> from) {
		return Codon.of((targetField, value) -> this.write(targetField, from.apply(value)), (sourceField, value) -> into.apply(this.parse(sourceField, value)));
	}
	
	/**
	 * Transforms this Codon&lt;T&gt; into a Codon&lt;Set&lt;T&gt;&gt;.
	 * The set is immutable, I think.
	 * Uses comma separation.
	 */
	default Codon<Set<T>> setOf() {
		return Codon.of((targetField, set) ->
			set.stream()
				.map(e -> this.write(targetField, e))
				.collect(Collectors.joining(", ")),
			(sourceField, value) -> Arrays.stream(value.split(","))
				.map(String::trim)
				//.filter(s -> !s.isEmpty())
				.map(s -> this.parse(sourceField, s))
				.collect(Collectors.toSet()));
	}
	
	/**
	 * Transforms this Codon&lt;T&gt; into a Codon&lt;List&lt;T&gt;&gt;.
	 * The list is immutable, I think.
	 * Uses comma separation.
	 */
	default Codon<List<T>> listOf() {
		return Codon.of(
			(targetField, list) ->
				list.stream()
					.map(e -> this.write(targetField, e))
					.collect(Collectors.joining(", ")),
			(sourceField, value) ->
				Arrays.stream(value.split(","))
					.map(String::trim)
					//.filter(s -> !s.isEmpty())
					.map(s -> this.parse(sourceField, s))
					.collect(Collectors.toList()));
	}
	
	/**
	 * Transforms this Codon&lt;T&gt; into a Codon&lt;Optional&lt;T&gt;&gt;.
	 * Empty strings are Optional.empty(), nonempty strings delegate to the original codon
	 */
	default Codon<Optional<T>> optionalOf() {
		return Codon.of(
			(targetField, opt) -> opt.map(x -> this.write(targetField, x)).orElse(""),
			(sourceField, value) -> value.isEmpty() ? Optional.empty() : Optional.of(this.parse(sourceField, value)));
	}
	
	@SuppressWarnings("unchecked")
	default Codon<T[]> arrayOf(Class<?> remindMeWhatMyComponentTypeWasPlease) {
		return listOf().dimap(
			list -> list.toArray((T[]) Array.newInstance(remindMeWhatMyComponentTypeWasPlease, 0)),
			Arrays::asList);
	}
	
	/**
	 * A codon for a numeric type
	 * @param name The name of the numeric type, used in error messages
	 * @param parser Something like Integer.parseInt
	 * @param atLeastExtractor Which field of the atLeast annotation is relevant for this type
	 * @param atMostExtractor Which field of the atMost annotation is relevant for this type
	 */
	static <T extends Comparable<T>> Codon<T> number(String name, Function<String, T> parser, Function<AtLeast, T> atLeastExtractor, Function<AtMost, T> atMostExtractor) {
		return Codon.ofToString((sourceField, value) -> {
			T x;
			try {
				x = parser.apply(value);
			} catch (RuntimeException e) {
				throw new ConfigParseException("Cannot parse " + value + " as an " + name, e);
			}
			
			AtLeast leastAnnotation = sourceField.getAnnotation(AtLeast.class);
			if(leastAnnotation != null) {
				T mustBeAtLeast = atLeastExtractor.apply(leastAnnotation);
				if(x.compareTo(mustBeAtLeast) < 0) {
					throw new ConfigParseException("Value " + x + " is not at least " + mustBeAtLeast);
				}
			}
			
			AtMost mostAnnotation = sourceField.getAnnotation(AtMost.class);
			if(mostAnnotation != null) {
				T mustBeAtMost = atMostExtractor.apply(mostAnnotation);
				if(x.compareTo(mustBeAtMost) > 0) {
					throw new ConfigParseException("Value " + x + " is not at most " + mustBeAtMost);
				}
			}
			
			return x;
		});
	}
}
