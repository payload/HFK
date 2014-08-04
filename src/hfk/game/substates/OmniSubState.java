/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hfk.game.substates;

import hfk.Explosion;
import hfk.IngameText;
import hfk.PointF;
import hfk.PointI;
import hfk.Shot;
import hfk.game.GameController;
import hfk.game.GameRenderer;
import hfk.game.InputMap;
import hfk.items.InventoryItem;
import hfk.mobs.Mob;
import java.util.LinkedList;
import java.util.ListIterator;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

/**
 *
 * @author LostMekka
 */
public class OmniSubState extends GameSubState {

	// this state is always present. of all other states, only one is present at a given time
	
	public OmniSubState(InputMap inputMap) {
		super(inputMap);
		inputMap.addKey(Input.KEY_M, InputMap.A_TOGGLEMUSIC);
	}

	@Override
	public void update(GameController ctrl, GameContainer gc, StateBasedGame sbg, int time) throws SlickException {
		ctrl.reduceScreenShake(time);
		if(getInputMap().isKeyPressed(InputMap.A_TOGGLEMUSIC)) ctrl.toggleMusic();
		// updates
		for(InventoryItem i1 : ctrl.items){
			i1.update(time);
			if(!i1.vel.isZero()) i1.pos.add(ctrl.level.doCollision(i1.pos, i1.size));
			boolean blockX = false;
			boolean blockY = false;
			PointF corr = new PointF();
			PointF i1Pos = i1.getTopLeftLabelPos();
			for(InventoryItem i2 : ctrl.items){
				if(i1 == i2) continue;
				PointF i2Pos = i2.getTopLeftLabelPos();
				PointF c = ctrl.level.doRectCollision(
						i1Pos.x, i1Pos.y, i1.labelSize.x, i1.labelSize.y, 
						i2Pos.x, i2Pos.y, i2.labelSize.x, i2.labelSize.y);
				if(corr.x * c.x < 0){
					blockX = true;
					corr.x = 0f;
				}
				if(!blockX && Math.abs(c.x) > Math.abs(corr.x)) corr.x = c.x;
				if(corr.y * c.y < 0f){
					blockY = true;
					corr.y = 0f;
				}
				if(!blockY && Math.abs(c.y) > Math.abs(corr.y)) corr.y = c.y;
			}
			i1.labelPos.add(corr);
		}
		ListIterator<IngameText> textIter = ctrl.texts.listIterator();
		while(textIter.hasNext()) if(textIter.next().update(time)) textIter.remove();
		for(Mob m : ctrl.mobs) if(!ctrl.mobsToRemove.contains(m)) m.update(time);
		for(Explosion e : ctrl.explosions) if(!ctrl.explosionsToRemove.contains(e)) e.update(time);
		
		for(Shot s : ctrl.shots) if(!ctrl.shotsToRemove.contains(s)){
			s.update(time);
			PointF corr = ctrl.level.doCollision(s.pos, s.size);
			if(!corr.isZero()){
				s.onCollideWithLevel(corr);
				continue;
			}
			for(Mob m : ctrl.mobs) if(!ctrl.mobsToRemove.contains(m)){
				if(m.pos.squaredDistanceTo(s.pos) <= (m.size + s.size) / 2f){
					if(s.onCollideWithMob(m)) break;
				}
			}
		}
	}

	@Override
	public void render(GameController ctrl, GameRenderer r, GameContainer gc) throws SlickException {
		PointF screenPos = ctrl.getScreenPos();
		ctrl.level.draw(
				(int)Math.floor(screenPos.x - 0.5f),
				(int)Math.floor(screenPos.y - 0.5f),
				(int)Math.ceil(ctrl.transformScreenToTiles(gc.getWidth()) + screenPos.x - 0.5f),
				(int)Math.ceil(ctrl.transformScreenToTiles(gc.getHeight()) + screenPos.y - 0.5f));
		for(InventoryItem i : ctrl.items) i.render();
		for(Mob m : ctrl.mobs) m.draw();
		for(Shot s : ctrl.shots) s.draw();
		for(Explosion e : ctrl.explosions) e.draw(r, gc);
		for(IngameText t : ctrl.texts) t.draw();
	}
	
}
