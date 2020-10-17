package org.lwjglb.engine.graph.LHMP;

import org.lwjglb.engine.graph.anim.AnimatedFrame;

import java.util.List;

public class LHMPAnimation {

    private String animationName;

    private int currentFrame;

    private List<AnimatedFrame> frames;

    private String name;

    private double duration;

    public LHMPAnimation(String animationName, List<AnimatedFrame> frames, double duration){
        this.animationName = animationName;
        this.currentFrame = 0;
        this.duration = duration;
        this.frames = frames;
    }

    public AnimatedFrame getCurrentFrame() {
        return this.frames.get(currentFrame);
    }

    public double getDuration() {
        return this.duration;
    }

    public List<AnimatedFrame> getFrames() {
        return frames;
    }

    public String getName() {
        return name;
    }

    public AnimatedFrame getNextFrame() {
        nextFrame();
        return this.frames.get(currentFrame);
    }

    public void nextFrame() {
        int nextFrame = currentFrame + 1;
        if (nextFrame > frames.size() - 1) {
            currentFrame = 0;
        } else {
            currentFrame = nextFrame;
        }
    }

}
