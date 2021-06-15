package agency.highlysuspect.libs.nacl.v1.types;

import agency.highlysuspect.libs.nacl.v1.ConfigParseException;
import agency.highlysuspect.libs.nacl.v1.annotation.Use;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

public class CodonTypeLookup {
	public static final Map<Type, Codon<?>> knownCodons = new HashMap<>();
	public static final Map<Type, Registry<?>> registeredTypes = new HashMap<>();
	public final Map<String, Codon<?>> namedCodons = new HashMap<>();
	
	static {
		knownCodons.put(String.class, Codon.STRING);
		knownCodons.put(Byte.TYPE, Codon.BYTE);
		knownCodons.put(Short.TYPE, Codon.SHORT);
		knownCodons.put(Integer.TYPE, Codon.INTEGER);
		knownCodons.put(Long.TYPE, Codon.LONG);
		knownCodons.put(Float.TYPE, Codon.FLOAT);
		knownCodons.put(Double.TYPE, Codon.DOUBLE);
		knownCodons.put(Boolean.TYPE, Codon.BOOLEAN);
		knownCodons.put(Byte.class, Codon.BYTE);
		knownCodons.put(Short.class, Codon.SHORT);
		knownCodons.put(Integer.class, Codon.INTEGER);
		knownCodons.put(Long.class, Codon.LONG);
		knownCodons.put(Float.class, Codon.FLOAT);
		knownCodons.put(Double.class, Codon.DOUBLE);
		knownCodons.put(Boolean.class, Codon.BOOLEAN);
		knownCodons.put(Identifier.class, Codon.IDENTIFIER);
		
		try {
			//1. Iterate over the static fields in Registry.class.
			//2. Pluck out all the fields of type "Registry<Something>" or "Registry<? extends Something>"
			//3. In registeredTypes, record that the type Something is recorded in that registry
			// (I can use this information to serialize these types, by looking their ID up in the Registry.)
			//
			//note 1: I'd love to iterate over the members of Registry.ROOT, but I don't think recovering the registered type is possible.
			//Maybe something silly like "take an item out of the registry and see what type it is" is possible.
			//
			//note 2: Registry.CUSTOM_STAT is a Registry<Identifier>, so, that's kinda odd.
			for(Field field : Registry.class.getDeclaredFields()) {
				if((field.getModifiers() & Opcodes.ACC_STATIC) != 0 && Registry.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType param) {
					Type arg = param.getActualTypeArguments()[0];
					if(arg instanceof WildcardType wildcard && wildcard.getUpperBounds()[0] instanceof ParameterizedType heck) {
						//Registry<? extends Something>
						//(pracitcally speaking this is only the Registry<? extends Registry<?>> registry)
						registeredTypes.put(heck.getRawType(), (Registry<?>) field.get(null));
					} else if(arg instanceof Class<?> classs) {
						//Registry<Something>
						registeredTypes.put(classs, (Registry<?>) field.get(null));
					}
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Problem reflecting Registry.class", e);
		}
	}
	
	public void registerNamedCodon(String name, Codon<?> codon) {
		namedCodons.put(name, codon);
	}
	
	public <T> Codon<T> find(Field field) {
		Use use = field.getAnnotation(Use.class);
		if(use != null) {
			Codon<T> codon = (Codon<T>) namedCodons.get(use.value());
			if(codon == null) throw new ConfigParseException("No codon named " + use.value());
			else return codon;
		}
		
		return findType(field.getGenericType());
	}
	
	public <T> Codon<T> findType(Type type) {
		//Simple types
		if(type instanceof Class<?>) {
			if(knownCodons.containsKey(type)) {
				return (Codon<T>) knownCodons.get(type);
			}
			
			if(registeredTypes.containsKey(type)) {
				return (Codon<T>) Codon.registryEntry(registeredTypes.get(type));
			}
		}
		
		//Collections
		if(type instanceof ParameterizedType param && param.getRawType() instanceof Class<?> outer) {
			Codon<?> innerCodon = findType(param.getActualTypeArguments()[0]);
			if(innerCodon != null) {
				if(List.class.isAssignableFrom(outer)) {
					return (Codon<T>) innerCodon.listOf();
				}
				
				if(Set.class.isAssignableFrom(outer)) {
					return (Codon<T>) innerCodon.setOf();
				}
				
				if(Optional.class.isAssignableFrom(outer)) {
					return (Codon<T>) innerCodon.optionalOf();
				}
			}
		}
		
		throw new ConfigParseException("Cannot find codon for type " + type.getTypeName());
	}
}
