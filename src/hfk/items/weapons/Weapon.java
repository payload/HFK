/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hfk.items.weapons;

import hfk.PointF;
import hfk.Shot;
import hfk.game.GameController;
import hfk.game.GameRenderer;
import hfk.game.Resources;
import hfk.items.AmmoItem;
import hfk.items.InventoryItem;
import hfk.mobs.Mob;
import hfk.stats.Damage;
import hfk.stats.DamageCard;
import hfk.stats.ItemEffect;
import hfk.stats.WeaponStatsCard;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.Sound;

/**
 *
 * @author LostMekka
 */
public abstract class Weapon extends InventoryItem {

	public static class WeaponType{
		// generic types
		public static final WeaponType none = new WeaponType("!!no type!!");
		public static final WeaponType cheatWeapon = new WeaponType("cheat weapon");
		public static final WeaponType explosiveWeapon = new WeaponType("explosive weapon");
		public static final WeaponType energyWeapon = new WeaponType("energy weapon");
		public static final WeaponType plasmaWeapon = new WeaponType("plasma weapon");
		public static final WeaponType zoomable = new WeaponType("zoomable");
		public static final WeaponType shotgun = new WeaponType("shotgun");
		public static final WeaponType machinegun = new WeaponType("machinegun");
		public static final WeaponType automatic = new WeaponType("automatic weapon");
		public static final WeaponType singleReload = new WeaponType("single reload");
		// concrete types
		public static final WeaponType autoShotgun = new WeaponType("auto shotgun");
		public static final WeaponType cheatRifle = new WeaponType("cheat rifle");
		public static final WeaponType damagedHuntingGun = new WeaponType("damaged hunting gun");
		public static final WeaponType doubleBarrelShotgun = new WeaponType("double barrel shotgun");
		public static final WeaponType energyPistol = new WeaponType("energy pistol");
		public static final WeaponType grenadeLauncher = new WeaponType("grenade launcher");
		public static final WeaponType pistol = new WeaponType("pistol");
		public static final WeaponType plasmaMachinegun = new WeaponType("plasma machinegun");
		public static final WeaponType plasmaStorm = new WeaponType("plasma storm");
		public static final WeaponType pumpActionShotgun = new WeaponType("pump action shotgun");
		public static final WeaponType rocketLauncher = new WeaponType("rocket launcher");
		public static final WeaponType sniperRifle = new WeaponType("sniper rifle");
		// init parents
		static { 
			// generic types
			machinegun.setParents(new WeaponType[]{automatic});
			// concrete types
			cheatRifle.setParents(new WeaponType[]{cheatWeapon, grenadeLauncher});
			autoShotgun.setParents(new WeaponType[]{shotgun, automatic, singleReload});
			doubleBarrelShotgun.setParents(new WeaponType[]{shotgun});
			energyPistol.setParents(new WeaponType[]{pistol, energyWeapon});
			grenadeLauncher.setParents(new WeaponType[]{explosiveWeapon});
			plasmaMachinegun.setParents(new WeaponType[]{machinegun, plasmaWeapon});
			plasmaStorm.setParents(new WeaponType[]{machinegun, plasmaWeapon});
			pumpActionShotgun.setParents(new WeaponType[]{shotgun, singleReload});
			rocketLauncher.setParents(new WeaponType[]{explosiveWeapon});
			sniperRifle.setParents(new WeaponType[]{zoomable, singleReload});
		}
		
		private final String name;
		private WeaponType[] parents = null;
		private WeaponType(String name){ 
			this.name = name;
		}
		private void setParents(WeaponType[] parents) {
			this.parents = parents;
			// test for cycles in parent relations
			for(WeaponType parent : parents){
				WeaponType[] path = parent.detectCycles(this, new WeaponType[0]);
				if(path != null){
					String s = "cycle in weapon type parent relation detected: ";
					s += toString() + " -> ";
					for(WeaponType t : path) s += t.toString() + " -> ";
					s += toString();
					throw new RuntimeException(s);
				}
			}
		}
		public WeaponType[] detectCycles(WeaponType t, WeaponType[] path){
			if(this == t) return path;
			if(parents != null) for(WeaponType parent : parents){
				WeaponType[] newPath = new WeaponType[path.length+1];
				System.arraycopy(path, 0, newPath, 0, path.length);
				newPath[path.length] = this;
				WeaponType[] ans = parent.detectCycles(t, newPath);
				if(ans != null) return ans;
			}
			return null;
		}
		@Override
		public String toString(){
			return name;
		}
		public boolean isSubTypeOf(WeaponType t){
			if(this == t) return true;
			if(parents != null) for(WeaponType parent : parents) if(parent.isSubTypeOf(t)) return true;
			return false;
		}
	}
	
	public static enum AmmoType {
		bullet {
			@Override public String getShortID(){ return "b"; }
		}, 
		shell {
			@Override public String getShortID(){ return "sh"; }
		}, 
		plasmaRound {
			@Override public String getShortID(){ return "p"; }
			@Override public String toString() { return "plasma round"; }
		}, 
		sniperRound {
			@Override public String getShortID(){ return "sr"; }
			@Override public String toString() { return "sniper round"; }
		}, 
		grenade {
			@Override public String getShortID(){ return "g"; }
		}, 
		rocket {
			@Override public String getShortID(){ return "r"; }
		}, 
		energy {
			@Override public String getShortID(){ return "e"; }
		}, 
		;
		public abstract String getShortID();
	}
	public static final int AMMO_TYPE_COUNT = AmmoType.values().length;
	
	public enum WeaponState { ready, cooldownShot, cooldownBurst, cooldownReload }
	
	public WeaponType type = WeaponType.none;
	public Mob bionicParent = null;
	public float currentScatter;
	public float weaponLength = 0.5f, lengthOffset = 0.3f;
	public Image img, flippedImg;
	public Sound shotSound = null, burstSound = null;
	public WeaponStatsCard basicStats, totalStats;
	public DamageCard basicDamageCard, totalDamageCard;
	public Shot.Team shotTeam = Shot.Team.dontcare;
	public Sound[] reloadStartSound = new Sound[AMMO_TYPE_COUNT];
	public Sound[] reloadEndSound = new Sound[AMMO_TYPE_COUNT];
	public Color displayColor = Color.yellow;
	public ItemEffect zoomEffect = null;
	
	private final int[] clips = new int[AMMO_TYPE_COUNT];
	private final float[] ammoRegenCounter = new float[AMMO_TYPE_COUNT];
	private WeaponState state = WeaponState.ready;
	private int clipToReload = -1, reloadAmount = 0, timer = 0, timerStart = 1, burstShotCount = 0, burstTimeBetweenShots = -1;
	private boolean reloadAll = true;
	private boolean zoom = false;
	

	public abstract Shot initShot(Shot s);
	public abstract WeaponStatsCard getDefaultWeaponStats();
	public abstract DamageCard getDefaultDamageCard();
	public abstract String getWeaponName();
	
	public float getScreenShakeAmount(){ return 0.1f; };
	public float getScreenRecoilAmount(){ return 0.2f; };
	
	public void weaponSelected(){}
	
	public void weaponUnSelected(){
		Mob m = getParentMob();
		// cancel reloading
		if(state == WeaponState.cooldownReload){
			InventoryItem i = new AmmoItem(pos.clone(), AmmoType.values()[clipToReload], reloadAmount);
			if(m != null && m.skills.shouldKeepAmmoOnCancelReload()) i = m.inventory.addItem(i);
			if(i != null) GameController.get().dropItem(i, null, false);
			setReady();
		}
		// do weapon specific stuff
		if(type.isSubTypeOf(WeaponType.zoomable)){
			if(m != null && zoom){
				zoom = false;
				effects.remove(zoomEffect);
				m.recalculateCards();
				if(m == GameController.get().player) GameController.get().recalcVisibleTiles = true;
			}
		}
	}
	
	public Weapon(float angle, PointF position) {
		super(position);
		this.angle = angle;
		basicStats = getDefaultWeaponStats();
		totalStats = basicStats.clone();
		currentScatter = totalStats.minScatter;
		System.arraycopy(totalStats.clipSize, 0, clips, 0, AMMO_TYPE_COUNT);
		basicDamageCard = getDefaultDamageCard();
		totalDamageCard = basicDamageCard.clone();
		reloadStartSound[AmmoType.plasmaRound.ordinal()] = Resources.getSound("reload_s_pr.wav");
		reloadEndSound[AmmoType.plasmaRound.ordinal()] = Resources.getSound("reload_e_pr.wav");
		destroyWhenUsed = false;
	}
	
	public void setImg(String ref){
		img = Resources.getImage(ref);
		flippedImg = Resources.getImage(ref, true);
	}
	
	private void setState(int timer, WeaponState s, boolean absolute){
		this.timer = absolute ? timer : (timer + this.timer);
		timerStart = this.timer;
		state = s;
	}
	
	private void setReady(){
		state = WeaponState.ready;
		timer = 0;
		timerStart = 1;
		clipToReload = -1;
		reloadAmount = 0;
		burstShotCount = 0;
		reloadAll = false;
	}

	@Override
	public Color getDisplayColor() {
		return displayColor;
	}
	
	@Override
	public String getDisplayName(){
		return String.format("%s %s %s", getWeaponName(), getShortAmmoString(false), getShortDamageString());
	}
	
	public String getShortAmmoString(boolean includeInventory){
		String s = "";
		boolean first = true;
		for(int i=0; i<AMMO_TYPE_COUNT; i++){
			if(totalStats.clipSize[i] > 0){
				if(first){
					first = false;
				} else {
					s += " ";
				}
				s += getShortAmmoString(i, includeInventory);
			}
		}
		return s;
	}
	
	public String getShortAmmoString(int ammoType, boolean includeInventory){
		if(totalStats.clipSize[ammoType] > 0){
			String ans = AmmoType.values()[ammoType].getShortID() + "(" + 
					clips[ammoType] + "/" + totalStats.clipSize[ammoType];
			if(includeInventory && parentInventory != null && ammoType != AmmoType.energy.ordinal()){
				ans += "/" + parentInventory.getAmmoCount(AmmoType.values()[ammoType]);
			}
			ans += ")";
			return ans;
		} else {
			return null;
		}
	}
	
	public String getAmmoString(int ammoType, boolean includeInventory){
		if(totalStats.clipSize[ammoType] > 0){
			String ans = AmmoType.values()[ammoType].toString() + ": (" + 
					clips[ammoType] + "/" + totalStats.clipSize[ammoType];
			if(includeInventory && parentInventory != null && ammoType != AmmoType.energy.ordinal()){
				ans += "/" + parentInventory.getAmmoCount(AmmoType.values()[ammoType]);
			}
			ans += ")";
			return ans;
		} else {
			return null;
		}
	}
	
	public String getShortDamageString(){
		String s = "";
		boolean first = true;
		for(int i=0; i<Damage.DAMAGE_TYPE_COUNT; i++){
			if(totalDamageCard.doesDamage(i)){
				if(first){
					first = false;
				} else {
					s += ", ";
				}
				s += totalDamageCard.getShortDamageString(i, false);
			}
		}
		return s;
	}

	@Override
	public boolean use(Mob m, boolean fromInventory) {
		if(fromInventory){
			return m.inventory.equipWeaponFromInventory(this);
		} else {
			return m.inventory.equipWeaponFromGround(this);
		}
	}

	public boolean isBionic(){
		return bionicParent != null;
	}
	
	public boolean isReloading(){
		return state == WeaponState.cooldownReload;
	}
	
	public float getProgress(){
		if(state == WeaponState.ready) return 0f;
		return (float)(timerStart - timer) / (float) timerStart;
	}
	
	public int getAmmoCount(AmmoType t){
		return clips[t.ordinal()];
	}
	
	public void aimAt(PointF target) {
		angle = (float)Math.atan2(target.y - pos.y, target.x - pos.x);
	}
	
	public boolean canFire(){
		return canFire(totalStats.shotsPerBurst);
	}
	
	private boolean canFire(int shotsPerBurst){
		for(int i=0; i<AMMO_TYPE_COUNT; i++){
			int a = Math.round(totalStats.ammoPerBurst[i]) + shotsPerBurst * Math.round(totalStats.ammoPerShot[i]);
			if(clips[i] < a) return false;
		}
		return isReady();
	}
	
	public boolean isReady(){
		return (state == WeaponState.ready);
	}
	
	public boolean reload(){
		if(!isReady()) return false;
		for(int i=0; i<AMMO_TYPE_COUNT; i++){
			if(canReload(i)){
				reloadAll = true;
				reloadInternal(i);
				return true;
			}
		}
		return false;
	}
	
	public boolean reload(AmmoType t){
		if(!isReady()) return false;
		int i = t.ordinal();
		if(canReload(i)){
			reloadInternal(t);
			return true;
		}
		return false;
	}
	
	private void reloadInternal(int i){
		reloadInternalBOTHVALUES(i, AmmoType.values()[i]);
	}
	
	private void reloadInternal(AmmoType t){
		reloadInternalBOTHVALUES(t.ordinal(), t);
	}
	
	private void reloadInternalBOTHVALUES(int i, AmmoType t){
		clipToReload = i;
		reloadAmount = Math.round(Math.min(totalStats.clipSize[i] - clips[i], totalStats.reloadCount[i]));
		if(!isBionic() && parentInventory != null){
			reloadAmount = Math.min(reloadAmount, parentInventory.getAmmoCount(t));
			parentInventory.removeAmmo(AmmoType.values()[i], reloadAmount);
		}
		setState(Math.round(totalStats.reloadTimes[i]), WeaponState.cooldownReload, true);
		Sound sound = reloadStartSound[i];
		if(sound != null) GameController.get().playSoundAt(sound, pos);
	}
	
	public boolean mustReload(){
		return mustReload(true, true);
	}
	
	public boolean mustReload(boolean ignoreRegenerating, boolean ignoreUnreloadable){
		for(int i=0; i<AMMO_TYPE_COUNT; i++){
			if(ignoreRegenerating && totalStats.ammoRegenerationRates[i] > 0) continue;
			if(ignoreUnreloadable && totalStats.reloadCount[i] <= 0) continue;
			if(clips[i] < totalStats.ammoPerBurst[i] + totalStats.shotsPerBurst * totalStats.ammoPerShot[i]) return true;
		}
		return false;
	}
	
	public boolean canReload(){
		if(!isReady()) return false;
		for(int i=0; i<AMMO_TYPE_COUNT; i++){
			if(i == AmmoType.energy.ordinal()) continue;
			if(canReload(i)) return true;
		}
		return false;
	}
	
	private boolean canReload(int i){
		return (totalStats.reloadCount[i] > 0 && clips[i] < totalStats.clipSize[i]
				&& (parentInventory == null || parentInventory.hasAmmo(AmmoType.values()[i])));
	}
	
	public final void pullAlternativeTrigger(){
		Mob m = getParentMob();
		if(m == null || m.skills.canAltFire(this)) pullAlternativeTriggerInternal();
	}
	
	public void pullAlternativeTriggerInternal(){
		if(type.isSubTypeOf(WeaponType.grenadeLauncher)){
			boolean foundLevel2Shot = false;
			for(Shot shot : GameController.get().shots){
				if(!GameController.get().isMarkedForRemoval(shot) && shot.parent == this){
					if(shot.manualDetonateLevel == 1) shot.hit();
					if(shot.manualDetonateLevel == 2 && ! foundLevel2Shot){
						shot.hit();
						foundLevel2Shot = true;
					}
				}
			}
		} else if(type.isSubTypeOf(WeaponType.shotgun)){
			pullTrigger(2, 100);
		} else if(type.isSubTypeOf(WeaponType.zoomable) && zoomEffect != null){
			Mob m = getParentMob();
			if(m != null){
				if(zoom){
					effects.remove(zoomEffect);
				} else {
					effects.add(zoomEffect);
				}
				zoom = !zoom;
				m.recalculateCards();
				if(m == GameController.get().player) GameController.get().recalcVisibleTiles = true;
			}
		}
	}
	
	public boolean pullTrigger(){
		return pullTrigger(totalStats.shotsPerBurst, Math.round(totalStats.shotInterval));
	}
	
	private boolean pullTrigger(int shotsPerBurst, int shotInterval){
		if(canFire(shotsPerBurst)){
			burstShotCount = shotsPerBurst;
			burstTimeBetweenShots = shotInterval;
			for(int i=0; i<AMMO_TYPE_COUNT; i++) clips[i] -= totalStats.ammoPerBurst[i];
			if(burstSound != null) burstSound.play();
			shootInternal();
			return true;
		}
		return false;
	}
	
	public float getScatteredAngle(){
		return angle + currentScatter / 180f * (float)Math.PI * (GameController.random.nextFloat() - 0.5f);
	}
	
	public void shootInternal(){
		if(getParentMob() == GameController.get().player){
			GameController.get().cameraShake(getScreenShakeAmount());
			GameController.get().cameraRecoil(angle + (float)Math.PI, getScreenRecoilAmount());
		}
		burstShotCount--;
		for(int i=0; i<AMMO_TYPE_COUNT; i++) clips[i] -= totalStats.ammoPerShot[i];
		if(shotSound != null) GameController.get().playSoundAt(shotSound, pos);
		for(int i=0; i<totalStats.projectilesPerShot; i++){
			Shot s = new Shot(this, null, null, 0.1f);
			s = initShot(s);
			s.dmg = totalDamageCard.createDamage();
			s.team = shotTeam;
			s.isGrenade = type.isSubTypeOf(WeaponType.grenadeLauncher);
			s.bounceCount = totalStats.shotBounces;
			s.bounceProbability = totalStats.bounceProbability;
			s.parent = this;
			Mob m = getParentMob();
			if(m != null) s = m.skills.modifyShot(s, this, m);
			GameController.get().shots.add(s);
		}
		if(burstShotCount > 0){
			setState(burstTimeBetweenShots, WeaponState.cooldownShot, false);
		} else {
			setState(mustReload() ? 0 : Math.round(totalStats.burstInterval), WeaponState.cooldownBurst, false);
		}
		currentScatter = Math.max(0, Math.min(totalStats.maxScatter, currentScatter + totalStats.scatterPerShot));
	}
	
	public Mob getParentMob(){
		Mob m = null;
		if(bionicParent != null){
			m = bionicParent;
		} else if(parentInventory != null) m = parentInventory.getParent();
		return (m != null && m.isAlive()) ? m : null;
	}
	
	@Override
	public void update(int time, boolean isEquipped, boolean isHeld){
		// accuracy cooldown
		currentScatter -= time / 1000f * totalStats.scatterCoolRate;
		if(currentScatter < totalStats.minScatter) currentScatter = totalStats.minScatter;
		if(currentScatter > totalStats.maxScatter) currentScatter = totalStats.maxScatter;
		// regenerate ammo
		for(int i=0; i<AMMO_TYPE_COUNT; i++){
			float rate = totalStats.ammoRegenerationRates[i];
			if(rate != 0f && (state != WeaponState.cooldownReload || clipToReload != i)){
				ammoRegenCounter[i] += rate * time / 1000f;
				int a = (int)ammoRegenCounter[i];
				ammoRegenCounter[i] -= a;
				clips[i] += a;
				if(clips[i] > totalStats.clipSize[i]) clips[i] = Math.round(totalStats.clipSize[i]);
			}
		}
		if(!isHeld){
			// fancy weapon updates are only done when the weapon is in an inventory or it is a bionic weapon
			super.update(time, isEquipped, isHeld);
			return;
		}
		// from here on: cooldown stuff, only when not ready!
		if(state == WeaponState.ready) return;
		timer -= time;
		if(timer <= 0){
			switch(state){
				case cooldownReload:
					clips[clipToReload] += reloadAmount;
					Sound sound = reloadEndSound[clipToReload];
					if(sound != null) GameController.get().playSoundAt(sound, pos);
					if(reloadAll){
						// try to reload other clips as well
						int t = -1;
						for(int i=clipToReload+1; i<AMMO_TYPE_COUNT; i++){
							if(canReload(i)){
								t = i;
								break;
							}
						}
						if(t != -1){
							reloadInternal(t);
							break;
						}
					}
					// nothing more to reload
					setReady();
					break;
				case cooldownShot:
					if(burstShotCount == 0) throw new RuntimeException("we are in the wrong state here!");
					shootInternal();
					break;
				case cooldownBurst:
					setReady();
					break;
			}
		}
	}
	
	@Override
	public void render(){
		PointF p = pos.clone();
		p.x += lengthOffset * Math.cos(angle);
		p.y += lengthOffset * Math.sin(angle);
		Mob m = getParentMob();
		boolean drawOutside = m == null || GameController.get().shouldDrawMobOutsideVisionRange(m);
		Image i = Math.abs(angle) > Math.PI/2 ? flippedImg : img;
		GameRenderer.LayerType l = parentInventory == null ? 
				GameRenderer.LayerType.items : GameRenderer.LayerType.mob2;
		GameController.get().renderer.drawImage(i, p, 1f, angle, drawOutside, l);
	}
	
	public void renderInformation(int x, int y, boolean colored){
		GameRenderer r = GameController.get().renderer;
		Color c = colored ? getDisplayColor() : GameRenderer.COLOR_TEXT_NORMAL;
		r.drawStringOnScreen("weapon : " + getWeaponName(), x, y, c, 1f);
		y += GameRenderer.MIN_TEXT_HEIGHT;
		DamageCard dc = totalDamageCard;
		x += 25;
		int l = 0;
		for(int i=0; i<Damage.DAMAGE_TYPE_COUNT; i++){
			if(colored) c = totalDamageCard.doesDamage(i) ? GameRenderer.COLOR_TEXT_NORMAL : GameRenderer.COLOR_TEXT_INACTIVE;
			String s = dc.getLongDamageString(i, true);
			int projectiles = totalStats.projectilesPerShot;
			if(projectiles > 1) s = "" + projectiles + " x " + s;
			l = Math.max(l, r.getStringWidth(s));
			r.drawStringOnScreen(s, x, y+i*GameRenderer.MIN_TEXT_HEIGHT, c, 1f);
		}
		x += l + 35;
		if(colored){
			if(isReady()){
				c = Color.green;
				if(canReload()) c = GameRenderer.COLOR_TEXT_NORMAL;
				if(mustReload(false, false)) c = Color.red;
			} else {
				c = GameRenderer.COLOR_TEXT_INACTIVE;
			}
		}
		for(int i=0; i<Weapon.AMMO_TYPE_COUNT; i++){
			String s = getAmmoString(i, true);
			if(s == null) continue;
			r.drawStringOnScreen(s, x, y, c, 1f);
			y += GameRenderer.MIN_TEXT_HEIGHT;
		}
	}
	
	public float getScatter(){
		return (currentScatter - totalStats.minScatter) / (totalStats.maxScatter - totalStats.minScatter);
	}
	
	public void resetStats(){
		totalDamageCard = basicDamageCard.clone();
		totalStats = basicStats.clone();
	}
	
	public void recalculateStats(){
		resetStats();
		Mob p = getParentMob();
		if(p == null) return;
		// calculate damage card
		DamageCard dcEffect = DamageCard.createBonus();
		p.addDamageCardEffects(dcEffect, this, p);
		totalDamageCard.applyBonus(dcEffect);
		// calculate weapon stats card
		WeaponStatsCard wscEffect = WeaponStatsCard.createBonus();
		p.addWeaponStatsCardEffects(wscEffect, this, p);
		totalStats.applyBonus(wscEffect);
	}
	
}
