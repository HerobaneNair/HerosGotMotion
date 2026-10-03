package hero.bane.herosgotmotion.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import hero.bane.herosgotmotion.config.HerosGotMotionScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HerosGotMotionScreen::new;
    }
}
