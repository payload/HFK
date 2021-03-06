/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hfk.game.substates;

import hfk.game.GameController;
import hfk.game.GameRenderer;
import hfk.game.InputMap;
import hfk.items.AmmoItem;
import hfk.items.EmptyItem;
import hfk.items.HealthPack;
import hfk.items.Inventory;
import hfk.items.InventoryItem;
import hfk.items.weapons.Weapon;
import hfk.menu.MenuItemList;
import hfk.menu.SimpleMenuBox;
import hfk.menu.SplitMenuBox;
import hfk.stats.Damage;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

/**
 *
 * @author LostMekka
 */
public class ExchangeSubState extends GameSubState{

	private Inventory invLeft = null, invRight = null;
	private InventoryItem selectedItem = null, markedItem = null;
	private boolean selectedIsLeft, markedIsLeft;
	private SplitMenuBox mb;
	private SimpleMenuBox mbLeft, mbRight, mbDescr;
	private MenuItemList<InventoryItem> listLeft, listRight;
	
	public ExchangeSubState(InputMap inputMap) {
		super(inputMap);
		inputMap.addKey(Input.KEY_ESCAPE, InputMap.A_EXCHANGE_CLOSE);
		inputMap.addKey(Input.KEY_UP, InputMap.A_EXCHANGE_UP);
		inputMap.addKey(Input.KEY_DOWN, InputMap.A_EXCHANGE_DOWN);
		inputMap.addMouseButton(Input.MOUSE_LEFT_BUTTON, InputMap.A_EXCHANGE_MOVE);
		inputMap.addMouseButton(Input.MOUSE_RIGHT_BUTTON, InputMap.A_EXCHANGE_SWAP);
	}

	public Inventory getLeftInventory() {
		return invLeft;
	}

	public Inventory getRightInventory() {
		return invRight;
	}

	public void init(Inventory il, Inventory ir){
		invLeft = il;
		invRight = ir;
		listLeft.unselect();
		listRight.unselect();
		populateInventoryLists();
	}
	
	private void populateInventoryLists(){
		boolean foundMarked = false;
		// left
		int sel = listLeft.getSelectedIndex();
		listLeft.clearList();
		for(InventoryItem i : invLeft.getList()){
			foundMarked |= i == markedItem;
			listLeft.addListItem(i, i.getDisplayName(), i.getDisplayColor(), i == markedItem);
		}
		listLeft.selectIndex(sel);
		// right
		sel = listRight.getSelectedIndex();
		listRight.clearList();
		for(InventoryItem i : invRight.getList()){
			foundMarked |= i == markedItem;
			listRight.addListItem(i, i.getDisplayName(), i.getDisplayColor(), i == markedItem);
		}
		listRight.selectIndex(sel);
		if(!foundMarked) markedItem = null;
	}
	
	@Override
	public void initAfterLoading(GameController ctrl, GameContainer gc) {
		int descLineCount = 1 + Math.max(Damage.DAMAGE_TYPE_COUNT, Weapon.AMMO_TYPE_COUNT);
		mb = new SplitMenuBox(gc, 1f, 0.5f);
		mbRight = new SimpleMenuBox(mb, SplitMenuBox.Location.topRight);
		listRight = new MenuItemList(mbRight, true, 'X');
		float s = SplitMenuBox.getSplitRatioFromSecondSize(gc.getHeight(), descLineCount*GameRenderer.MIN_TEXT_HEIGHT);
		SplitMenuBox smb2 = new SplitMenuBox(mb, SplitMenuBox.Location.topLeft, s, 1f);
		mbLeft = new SimpleMenuBox(smb2, SplitMenuBox.Location.topLeft);
		listLeft = new MenuItemList(mbLeft, true, 'X');
		mbDescr = new SimpleMenuBox(smb2, SplitMenuBox.Location.bottomLeft);
	}
	
	@Override
	public void update(GameController ctrl, GameContainer gc, StateBasedGame sbg, int time) throws SlickException {
		InputMap in = getInputMap();
		if(in.isKeyPressed(InputMap.A_OPEN_INVENTORY)){
			ctrl.viewInventory(ctrl.player.inventory);
			return;
		}
		if(in.isKeyPressed(InputMap.A_OPEN_SKILLS)){
			ctrl.viewSkills(ctrl.player.skills);
			return;
		}
		if(in.isKeyPressed(InputMap.A_EXCHANGE_CLOSE)){
			ctrl.setCurrSubState(ctrl.gameplaySubState);
			return;
		}
		int mx = gc.getInput().getMouseX();
		int my = gc.getInput().getMouseY();
		// handle scrolling
		if(mbLeft.isMouseInsideBox(mx, my)){
			if(in.isKeyPressed(InputMap.A_INV_UP) || in.getMouseWheelMove() > 0) listLeft.scroll(-1);
			if(in.isKeyPressed(InputMap.A_INV_DOWN) || in.getMouseWheelMove() < 0) listLeft.scroll(1);
		}
		if(mbRight.isMouseInsideBox(mx, my)){
			if(in.isKeyPressed(InputMap.A_INV_UP) || in.getMouseWheelMove() > 0) listRight.scroll(-1);
			if(in.isKeyPressed(InputMap.A_INV_DOWN) || in.getMouseWheelMove() < 0) listRight.scroll(1);
		}
		// handle selection
		listLeft.updateSelection(mx, my);
		listRight.updateSelection(mx, my);
		selectedItem = listLeft.getSelectedObject();
		selectedIsLeft = true;
		if(selectedItem == null){
			selectedItem = listRight.getSelectedObject();
			selectedIsLeft = false;
		}
		if(selectedItem instanceof EmptyItem){
			(selectedIsLeft ? listLeft : listRight).unselect();
			selectedItem = null;
		}
		// handle click input
		if(selectedItem != null){
			Inventory source = selectedIsLeft ? invLeft : invRight;
			Inventory target = selectedIsLeft ? invRight : invLeft;
			MenuItemList<InventoryItem> selectedList = selectedIsLeft ? listLeft : listRight;
			MenuItemList<InventoryItem> markedList = markedIsLeft ? listLeft : listRight;
			if(in.isMousePressed(InputMap.A_EXCHANGE_MOVE) && selectedItem != null){
				if(markedItem == null || selectedIsLeft == markedIsLeft){
					// simple move. ammo items are handled differently
					if(selectedItem instanceof AmmoItem){
						AmmoItem ai = (AmmoItem)selectedItem;
						Weapon.AmmoType t = ai.getAmmoType();
						int tTotal = target.getAmmoCount(t);
						int tStack = target.getMaxAmmoStackSize(t);
						int tLast = tTotal % tStack;
						int amount = Math.min(ai.getAmmoCount(), tStack - tLast);
						if(amount > 0){
							source.removeAmmo(t, amount);
							int back = target.addAmmo(t, amount);
							source.addAmmo(t, back);
							populateInventoryLists();
						}
					} else if(source.removeItem(selectedItem)){
						selectedItem = target.addItem(selectedItem);
						// add remaining item back to source inventory
						// (this could be remaining ammo or the whole item when target inv is full)
						if(selectedItem != null) source.addItem(selectedItem);
						populateInventoryLists();
					}
				} else {
					// exchange move
					if(source.removeItem(selectedItem)){
						if(target.removeItem(markedItem)){
							// removal successful. add items to opposite inventories
							markedList.setItemFlag(markedItem, 0, false);
							// the add calls should always return null,
							// since there is at least one free slot.
							InventoryItem i = target.addItem(selectedItem);
							if(i != null) System.out.println("WARNING (exchange) target inventory returned something other than null");
							i = source.addItem(markedItem);
							if(i != null) System.out.println("WARNING (exchange) source inventory returned something other than null");
							// clear item pointers, since they are invalid now
							markedItem = null;
							selectedItem = null;
							populateInventoryLists();
						} else {
							// cannot remove marked item, roll back!
							source.addItem(selectedItem);
						}
					}
				}
			}
			if(in.isMousePressed(InputMap.A_EXCHANGE_SWAP) && selectedItem != null){
				// mark item for exchange.
				// ammo items cannot be marked, because they are handled differently
				if(!(selectedItem instanceof AmmoItem)){
					// first, clear the marked one, if it exists
					if(markedItem != null) markedList.setItemFlag(markedItem, 0, false);
					if(selectedItem == markedItem){
						// tried to mark the already marked item. unmark it instead!
						markedItem = null;
					} else {
						// selected item is not the marked one. mark it!
						selectedList.setItemFlag(selectedItem, 0, true);
						markedItem = selectedItem;
						markedIsLeft = selectedIsLeft;
					}
				}
			}
		}
	}

	@Override
	public void render(GameController ctrl, GameRenderer r, GameContainer gc) throws SlickException {
		mb.render();
		renderDescription(r);
	}
	
	private void renderDescription(GameRenderer r){
		int x = mbDescr.getUsableX();
		int y = mbDescr.getUsableY();
		if(selectedItem == null){
			r.drawStringOnScreen("hover over an item to see a description here", 
					x, y, GameRenderer.COLOR_TEXT_INACTIVE, 1f);
			return;
		}
		if(selectedItem instanceof Weapon){
			((Weapon)selectedItem).renderInformation(x, y, true);
			return;
		}
		if(selectedItem instanceof HealthPack){
			HealthPack h = (HealthPack)selectedItem;
			r.drawStringOnScreen("health pack", x, y, 
					selectedItem.getDisplayColor(), 1f);
			y += GameRenderer.MIN_TEXT_HEIGHT;
			x += 25;
			r.drawStringOnScreen("heals for " + h.hp + " hp", x, y, 
					GameRenderer.COLOR_TEXT_NORMAL, 1f);
			return;
		}
		// no special description handler available. just print the item description string
		r.drawStringOnScreen(selectedItem.getDisplayName(), x, y, 
				selectedItem.getDisplayColor(), 1f);
	}
	
}
