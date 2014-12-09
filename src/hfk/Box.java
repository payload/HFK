package hfk;

import hfk.game.GameController;

/**
 *
 * @author LostMekka
 */
public class Box extends Shape{
	
	public int x,y,w,h;
	
	public Box() {}
	
	public Box(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}
	
	public Box(Box b) {
		this.x = b.x;
		this.y = b.y;
		this.w = b.w;
		this.h = b.h;
	}
	
	@Override
	public PointI getRandomPointInside() {
		return new PointI(GameController.random.nextInt(h)+x, GameController.random.nextInt(h)+y);
	}

	public boolean isInside(int px, int py){
		return	x+w > px && x <= px && y+h > py && y <= py;
	}

	public boolean touchesBox(Box b){
		return	x+w >= b.x && 
				x <= b.x+b.w && 
				y+h >= b.y && 
				y <= b.y+b.h;
	}

}

