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
	public final Map<Class<?>, Codon<?>> classyCodons = new HashMap<>();
	public final Map<String, Codon<?>> namedCodons = new HashMap<>();
	public static final Map<Type, Registry<?>> registryTypes = new HashMap<>();
	
	public void registerNamedCodon(String name, Codon<?> codon) {
		namedCodons.put(name, codon);
	}
	
	public void registerClassyCodon(Class<?> classs, Codon<?> codon) {
		classyCodons.put(classs, codon);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Codon<T> find(Field field) {
		Use use = field.getAnnotation(Use.class);
		if(use != null) {
			Codon<T> codon = (Codon<T>) namedCodons.get(use.value());
			if(codon == null) throw new ConfigParseException("No codon named " + use.value());
			else return codon;
		}
		
		return findType(field.getGenericType());
	}
	
	@SuppressWarnings("unchecked")
	public <T> Codon<T> findType(Type type) {
		//Simple types
		if(type instanceof Class<?>) {
			if(classyCodons.containsKey(type)) {
				return (Codon<T>) classyCodons.get(type);
			}
			
			if(registryTypes.containsKey(type)) {
				return (Codon<T>) Codon.registryEntry(registryTypes.get(type));
			}
		}
		
		//Collections / "type functions"
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
	
	{
		registerClassyCodon(String.class, Codon.STRING);
		registerClassyCodon(Byte.TYPE, Codon.BYTE);
		registerClassyCodon(Short.TYPE, Codon.SHORT);
		registerClassyCodon(Integer.TYPE, Codon.INTEGER);
		registerClassyCodon(Long.TYPE, Codon.LONG);
		registerClassyCodon(Float.TYPE, Codon.FLOAT);
		registerClassyCodon(Double.TYPE, Codon.DOUBLE);
		registerClassyCodon(Boolean.TYPE, Codon.BOOLEAN);
		registerClassyCodon(Byte.class, Codon.BYTE);
		registerClassyCodon(Short.class, Codon.SHORT);
		registerClassyCodon(Integer.class, Codon.INTEGER);
		registerClassyCodon(Long.class, Codon.LONG);
		registerClassyCodon(Float.class, Codon.FLOAT);
		registerClassyCodon(Double.class, Codon.DOUBLE);
		registerClassyCodon(Boolean.class, Codon.BOOLEAN);
		registerClassyCodon(Identifier.class, Codon.IDENTIFIER);
	}
	
	static {
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
						registryTypes.put(heck.getRawType(), (Registry<?>) field.get(null));
					} else if(arg instanceof Class<?> classs) {
						//Registry<Something>
						registryTypes.put(classs, (Registry<?>) field.get(null));
					}
				}
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Problem reflecting Registry.class", e);
		}
	}
}
