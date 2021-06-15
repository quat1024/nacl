package agency.highlysuspect.libs.nacl;

import agency.highlysuspect.libs.nacl.v1.ConfigExt;
import agency.highlysuspect.libs.nacl.v1.ConfigReader;
import agency.highlysuspect.libs.nacl.v1.annotation.AtLeast;
import agency.highlysuspect.libs.nacl.v1.annotation.Comment;
import agency.highlysuspect.libs.nacl.v1.annotation.Section;
import agency.highlysuspect.libs.nacl.v1.types.CodonTypeLookup;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Test implements ModInitializer {
	@Override
	public void onInitialize() {
		//lol yeah it's not an actual java testing framework. sorry
		
		ConfigReader reader = new ConfigReader();
		try {
			MyConfig config = reader.read(MyConfig.class, FabricLoader.getInstance().getConfigDir().resolve("test.cfg"));
			System.out.println(config.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class MyConfig implements ConfigExt {
		@Comment("Hello world! Field A")
		@AtLeast(intValue = 50)
		int fieldA = 100;
		
		@Comment("This is field B")
		Block fieldB = Blocks.STONE;
		
		@Section("The fun stuff")
		Set<Integer> intSet = ImmutableSet.of(1, 2, 3, 4, 5);
		
		List<Item> items = ImmutableList.of(Items.APPLE, Items.COOKIE);
		
		Float[] floats = new Float[] {5f, 10f, 0.1f + 0.2f, 100000.002f};
		
		@Override
		public void upgrade(HashMap<String, String> unknownKeys) {
			unknownKeys.forEach((k, v) -> {
				System.err.println("Unknown key value: " + k + ":" + v);
			});
		}
	}
}
