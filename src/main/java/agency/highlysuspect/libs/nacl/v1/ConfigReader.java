package agency.highlysuspect.libs.nacl.v1;

import agency.highlysuspect.libs.nacl.v1.annotation.*;
import agency.highlysuspect.libs.nacl.v1.types.Codon;
import agency.highlysuspect.libs.nacl.v1.types.CodonTypeLookup;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigReader {
	public ConfigReader() {
		this.typeLookup = new CodonTypeLookup();
	}
	
	public final CodonTypeLookup typeLookup;
	
	public <T> T read(Class<T> configClass, Path configPath) throws IOException {
		T configInst;
		if(Files.exists(configPath)) {
			//The config file exists. Parse it from disk
			configInst = parse(configClass, configPath);
			//It might be old, so call upgrade() to let the config decide what to do
			upgrade(configInst);
			
		} else {
			//Create a default config instance
			configInst = defaultInstance(configClass);
		}
		
		save(configClass, configInst, configPath); //Always save over the file
		finish(configInst);
		return configInst;
	}
	
	protected <T> T defaultInstance(Class<T> configClass) {
		try {
			return configClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Config class " + configClass.toGenericString() + " doesn't have a zero argument constructor (or a problem when calling it)", e);
		}
	}
	
	protected <T> void upgrade(T configInst) {
		if(configInst instanceof ConfigExt ext) ext.upgrade();
	}
	
	protected <T> void finish(T configInst) {
		if(configInst instanceof ConfigExt ext) ext.finish();
	}
	
	protected <T> T parse(Class<T> configClass, Path configPath) throws IOException {
		List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
		
		T configInst = defaultInstance(configClass);
		
		for(int lineNo = 0; lineNo < lines.size(); lineNo++) {
			String line = lines.get(lineNo).trim();
			
			//Skip blank lines and comments
			if(line.isEmpty() || line.startsWith("#")) continue;
			
			//Config file entries are of the form "key: value", so there has to be a colon character somewhere on the line.
			int colonIdx = line.indexOf(':');
			if(colonIdx == -1) {
				throw new ConfigParseException("No colon character on line " + lineNo + " in config file " + configPath);
			}
			
			String key = line.substring(0, colonIdx).trim();
			String value = line.substring(colonIdx + 1).trim();
			
			//Find the field associated with this key
			Field keyField = findConfigField(configClass, key);
			if(keyField == null) {
				//It's possible the config file format has changed, and this field is no longer relevant
				//Ask the config what to do about it.
				handleUnknownField(key, value);
				continue;
			}
			
			keyField.setAccessible(true);
			Codon<?> codon = typeLookup.find(keyField);
			
			try {
				keyField.set(configInst, codon.parse(keyField, value));
			} catch (ReflectiveOperationException e) {
				throw new ConfigParseException("problem setting field", e);
			}
		}
		
		return configInst;
	}
	
	protected @Nullable <T> Field findConfigField(Class<T> configClass, String name) {
		try {
			Field field = configClass.getDeclaredField(name);
			return skipField(field) ? null : field;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected boolean skipField(Field field) {
		return ((field.getModifiers() & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT)) != 0) || field.getAnnotation(Skip.class) != null;
	}
	
	protected void handleUnknownField(String key, String value) {
		//TODO
	}
	
	@VisibleForTesting
	public <T> List<String> stringify(Class<T> configClass, T configInst) {
		T defaultConfig = defaultInstance(configClass); //For writing the "default: ____" comments
		
		List<String> lines = new ArrayList<>();
		
		for(Field field : configClass.getDeclaredFields()) {
			if(skipField(field)) continue;
			field.setAccessible(true);
			
			BlankLine bl = field.getDeclaredAnnotation(BlankLine.class);
			if(bl != null) for(int i = 0; i < bl.lines(); i++) lines.add("");
			
			Section sect = field.getDeclaredAnnotation(Section.class);
			if(sect != null) {
				String title = sect.value();
				String bar = StringUtils.repeat('#', title.length() + 6);
				lines.add(bar);                   // ################
				lines.add("## " + title + " ##"); // ## My Section ##
				lines.add(bar);                   // ################
				lines.add("");                    // 
			}
			
			Comment comment = field.getDeclaredAnnotation(Comment.class);
			if(comment != null) {
				for(String c : comment.value()) {
					lines.add("# " + c);
				}
			}
			
			Example example = field.getDeclaredAnnotation(Example.class);
			if(example != null) {
				for(String c : example.value()) {
					lines.add("# Example: " + c);
				}
			}
			
			Note note = field.getDeclaredAnnotation(Note.class);
			if(note != null) {
				boolean first = true;
				for(String noteLine : note.value()) {
					lines.add((first ? "# Note: " : "#       ") + noteLine);
					first = false;
				}
			}
			
			//todo write atLeast, atMost, etc
			
			Codon<?> codon = typeLookup.find(field);
			try {
				String defaultValue = codon.writeErased(field, field.get(defaultConfig));
				lines.add("# Default: " + (defaultValue.isEmpty() ? "<empty>" : defaultValue));
				
				lines.add(field.getName() + ": " + codon.writeErased(field, field.get(configInst)));
			} catch (ReflectiveOperationException e) {
				throw new ConfigParseException("reflective kaboom", e);
			}
			
			//blank line after the field
			lines.add("");
		}
		
		return lines;
	}
	
	protected <T> void save(Class<T> configClass, T configInst, Path configPath) throws IOException {
		Files.write(configPath, stringify(configClass, configInst));
	}
}
