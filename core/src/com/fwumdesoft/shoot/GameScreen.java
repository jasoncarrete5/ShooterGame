package com.fwumdesoft.shoot;

import static com.fwumdesoft.shoot.net.NetConstants.*;
import java.nio.ByteBuffer;
import java.util.UUID;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.fwumdesoft.shoot.model.NetActor;
import com.fwumdesoft.shoot.model.Player;
import com.fwumdesoft.shoot.net.ServerInterface;

public class GameScreen extends ScreenAdapter {
	private Stage stage;
	/** The player controlled by the computer running the application. */
	private Player localPlayer;
	
	private Thread netReceiveThread;
	
	@Override
	public void show() {
		FillViewport viewport = new FillViewport(500, 500f * ((float)Gdx.graphics.getHeight() / Gdx.graphics.getWidth()));
		stage = new Stage(viewport);
		Gdx.input.setInputProcessor(stage);
		
		startNetReceiveThread();
		
		localPlayer = new Player(ServerInterface.getClientId(), true);
		localPlayer.addListener(new InputManager(localPlayer));
		stage.setKeyboardFocus(localPlayer);
		stage.addActor(localPlayer);
		localPlayer.setPosition(viewport.getWorldWidth()/2, viewport.getWorldHeight()/2, Align.center);
	}
	
	/**
	 * Starts the net receive thread to begin receiving data from the server.
	 */
	private void startNetReceiveThread() {
		netReceiveThread = new Thread(() -> {
			while(!Thread.interrupted()) {
				if(!ServerInterface.isConnected())
					continue;
				ByteBuffer buffer = ServerInterface.receiveData();
				if(buffer == null) {
					Gdx.app.debug("GameScreen", "Skipped a message from the server");
					continue;
				}
				
				@SuppressWarnings("unused")
				final int dataLength = buffer.getInt();
				final byte msgId = buffer.get();
				final UUID senderId = new UUID(buffer.getLong(), buffer.getLong());
				final ByteBuffer data = buffer;
				
				Gdx.app.log("GameScreen", msgId + "");
				
				switch(msgId)
				{
				case MSG_CONNECT:
					Player newPlayer = new Player(senderId);
					stage.addActor(newPlayer);
					Gdx.app.log("GameScreen", "Added a new player to the stage ID: " + senderId);
					break;
				case MSG_DISCONNECT:
					Actor removedActor = null;
					for(Actor a : stage.getActors()) {
						if(a instanceof NetActor) {
							NetActor netActor = (NetActor)a;
							if(senderId.equals(netActor.getNetId())) {
								removedActor = netActor;
								break;
							}
						}
					}
					if(removedActor != null) {
						removedActor.remove();
					}
					Gdx.app.log("GameScreen", "Removed a player with ID: " + senderId + " from the stage");
					break;
				case MSG_UPDATE_PLAYER:
					for(Actor a : stage.getActors()) {
						if(a instanceof Player) {
							Player player = (Player)a;
							if(senderId.equals(player.getNetId())) {
								player.setX(data.getFloat());
								player.setY(data.getFloat());
								player.setRotation(data.getFloat());
								Gdx.app.debug("GameScreen", "player ID: " + player.getNetId() + " " + player.getX() + " " + player.getY() + " rot: "
										+ player.getRotation());
							}
						}
					}
					Gdx.app.debug("GameScreen", "Updated Player ID: " + senderId);
					break;
				case MSG_UPDATE:
					UUID netId = new UUID(data.getLong(), data.getLong());
					float x = data.getFloat();
					float y = data.getFloat();
					float rot = data.getFloat();
					for(Actor a : stage.getActors()) {
						if(a instanceof NetActor) {
							NetActor n = (NetActor)a;
							if(n.getNetId().equals(netId)) {
								n.setPosition(x, y);
								n.setRotation(rot);
								break;
							}
						}
					}
					
					Gdx.app.debug("GameScreen", "Updated NetActor ID: " + netId);
					break;
				case MSG_REMOVE_BOLT:
					UUID boltNetId = new UUID(data.getLong(), data.getLong());
					UUID targetId = new UUID(data.getLong(), data.getLong());
					
					//remove bolt from stage
					Actor removed = null;
					synchronized(stage) {
						for(Actor a : stage.getActors()) {
							if(a instanceof NetActor) {
								NetActor n = (NetActor)a;
								if(n.getNetId().equals(boltNetId)) {
									removed = n;
									break;
								}
							}
						}
					}
					if(removed != null)
						removed.remove();
					
					if(!targetId.equals(NULL_ID)) {
						//TODO do something if the bolt hit a player
					}
					
					Gdx.app.log("GameScreen", "Received MSG_REMOVE_BOLT");
					break;
				}
			}
		}, "net_receive_thread");
		netReceiveThread.start();
	}
	
	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		stage.getBatch().begin();
		stage.getBatch().draw(Main.assets.get("textures/background.png", Texture.class), 0, 0);
		stage.getBatch().end();
		
		stage.act(delta);
		stage.draw();
		
		if(ServerInterface.isConnected())
			ServerInterface.heartbeat();
	}
	
	@Override
	public void hide() {
		dispose();
	}
	
	@Override
	public void dispose() {
		stage.dispose();
		netReceiveThread.interrupt();
		Gdx.app.log("GameScreen", "Disposed");
	}
}
