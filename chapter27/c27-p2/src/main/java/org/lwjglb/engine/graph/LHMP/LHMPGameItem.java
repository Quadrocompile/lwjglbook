package org.lwjglb.engine.graph.LHMP;

import org.lwjglb.engine.graph.Material;
import org.lwjglb.engine.graph.Texture;
import org.lwjglb.engine.graph.anim.Animation;
import org.lwjglb.engine.items.GameItem;

public class LHMPGameItem extends GameItem {

    private String id;
    private LHMPAnimation currentAnimation;

    private LHMPBaseMesh.AnimationClip currentAnimationClip;

    private Animation legacyAnimation;

    public LHMPGameItem(LHMPBaseMesh baseMesh, Texture texture, String id){
        super(baseMesh.mesh);
        baseMesh.mesh.setMaterial(new Material(texture));
        this.id = id;
    }

    public void setAnimation(LHMPAnimation currentAnimation){
        this.currentAnimation = currentAnimation;
    }

    public void setcurrentAnimationClip(LHMPBaseMesh.AnimationClip currentAnimation){
        this.currentAnimationClip = currentAnimation;
    }

    public void setLegacyAnimation(Animation legacyAnimation){
        this.legacyAnimation = legacyAnimation;
    }

    public String getID() {
        return id;
    }

    public LHMPAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    public LHMPBaseMesh.AnimationClip getCurrentAnimationClip() {
        return currentAnimationClip;
    }

    public Animation getLegacyAnimation() {
        return legacyAnimation;
    }

}
