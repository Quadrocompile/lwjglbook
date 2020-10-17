package org.lwjglb.engine.graph.anim;

import java.util.Map;
import java.util.Optional;

import org.lwjglb.engine.graph.Mesh;
import org.lwjglb.engine.items.GameItem;

public class AnimGameItem extends GameItem {

    private Map<String, Animation> animations;

    private Animation currentAnimation;

    private String id;

    public AnimGameItem(Mesh[] meshes, Map<String, Animation> animations, String id) {
        super(meshes);
        this.animations = animations;
        Optional<Map.Entry<String, Animation>> entry = animations.entrySet().stream().findFirst();
        currentAnimation = entry.isPresent() ? entry.get().getValue() : null;

        this.id = id;
    }

    public String getID() { return id; }

    public Animation getAnimation(String name) {
        return animations.get(name);
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public void setCurrentAnimation(Animation currentAnimation) {
        this.currentAnimation = currentAnimation;
    }
}
