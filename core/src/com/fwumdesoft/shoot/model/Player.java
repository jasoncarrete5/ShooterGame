package com.fwumdesoft.shoot.model;

import java.util.UUID;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.fwumdesoft.shoot.Main;
import com.fwumdesoft.shoot.net.ServerInterface;

/**
 * Represents a player in the game that can move around the world.
 * Players are controlled by each client and the client primarily has
 * authority over the position of the player.
 */
public class Player extends NetActor {
	private float speed = 5;
	public static final float ROTATE_SPEED = 3f;
	
	private TextureRegion texture;
	private boolean isLocalPlayer;
	
	public Player(final UUID id) {
		setNetId(id);
		setWidth(40);
		setHeight(40);
		setOrigin(Align.center);
		if(Gdx.app.getType() != ApplicationType.HeadlessDesktop)
			texture = new TextureRegion(Main.assets.get("textures/player.png", Texture.class));
	}
	
	public Player(final UUID id, boolean isLocalPlayer) {
		this(id);
		this.isLocalPlayer = isLocalPlayer;
	}
	
	@Override
	public void draw(Batch batch, float parentAlpha) {
		batch.draw(texture, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
	}
	
	@Override
	public void positionChanged(float deltaX, float deltaY) {
		if(isLocalPlayer()) {
			if(ServerInterface.isConnected()) {
				ServerInterface.updateLocalPlayer(this);
			}
			
			getStage().getCamera().position.set(getX() + getOriginX(), getY() + getOriginY(), 1);
			
//			Gdx.app.log("Player", getX() + ", " + getY());
		}
	}
	
	@Override
	public void rotationChanged(float deltaRot) {
		if(isLocalPlayer()) {
			if(ServerInterface.isConnected()) {
				ServerInterface.updateLocalPlayer(this);
			}
		}
	}
	
	public Player setSpeed(float newSpeed) {
		speed = newSpeed;
		return this;
	}
	
	public float getSpeed() {
		return speed;
	}
	
	/**
	 * Gets this player's velocity x-component relative to the forward direction of this player.
	 * @return The x-component of velocity.
	 */
	public float getSpeedCompX() {
		return speed * MathUtils.cosDeg(getRotation());
	}
	
	/**
	 * Gets this player's velocity y-component relative to the forward direction of this player.
	 * @return The y-component of velocity.
	 */
	public float getSpeedCompY() {
		return speed * MathUtils.sinDeg(getRotation());
	}
	
	public boolean isLocalPlayer() {
		return isLocalPlayer;
	}
}
