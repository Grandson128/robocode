package balaserodinhas;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class Colatra extends AdvancedRobot{

	int moveDirection = 1;	//Variable to handle movement direction
	double countTurns = 0;	//Variable to keep track of turns
	double oldEnergy=100;	//Variable to hold enemy robot energy
	float bodyColor=0, gunColor=0, radarColor=0; //Variables to handle robot colors
	
	/**
	 * Function responsible for the robot's initial behavior
	 */
	public void initBehavior() {
		setAdjustGunForRobotTurn(true); //Sets the gun to turn independent from the robot's turn. 
		setAdjustRadarForGunTurn(true);	//Sets the radar to turn independent from the gun's turn. 
		
		if(getRadarTurnRemaining() == 0.0) {	//Checks if radar is still
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);	//Starts rotating the radar until some robot is canned
		}
		
		setAhead(4000);
	}
	
	/**
	 * Function checks if robot is still, if true, inverts the movement direction
	 */
	public void ifRobotStopped() {
		if(getVelocity() == 0) {
			moveDirection = -moveDirection;	//Invert direction
		}
	}
	
	/**
	 * Function to activate robot RGB components
	 * Just like desktops, RGB will give us an HUGE performance advantage
	 */
	public void makeRobotRGB() {
		if(bodyColor > 1) { bodyColor = 0;}
		if(gunColor > 1) { gunColor = 0;}
		if(radarColor > 1) { radarColor = 0;}
	
		setColors(Color.getHSBColor(bodyColor, 1, 1), Color.getHSBColor(gunColor, 1, 1), Color.getHSBColor(radarColor, 1, 1));
		bodyColor+= 0.05;
		gunColor+=0.1;
		radarColor+=0.01;
	}
	
	/**
	 *The main method in every robot.
	 */
	public void run() {
		while(true) {
			makeRobotRGB();
			countTurns++;
			if(countTurns == 1) {
				initBehavior();
			}
			execute();	//Executes any actions
		}
	}
	
	/**
	 *This method is called when your robot sees another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {

		double angleToEnemy = getHeadingRadians() + e.getBearingRadians();	//angle to the scanned robot
		double distanceToEnemy = e.getDistance();	//Distance to the enemy robot
		
		/**
		 *RADAR
		 */
		double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians());	//angle to turn radar
		double extraTurn = Math.min(Math.atan(24.0 / e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);
		radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);	//increase or decrease the radar radius
		setTurnRadarRightRadians(radarTurn); //turn radar
		
		/**
		 *GUN
		 */
		double enemyLastVelocity=e.getVelocity() * Math.sin(e.getHeadingRadians() -angleToEnemy);	//enemy later velocity
		double aimTurn = robocode.util.Utils.normalRelativeAngle(angleToEnemy- getGunHeadingRadians()+enemyLastVelocity/22);	//amount to turn our gun, lead just a little bit
		setTurnGunRightRadians(aimTurn); //turn our gun
		
		
		moveAndShoot(distanceToEnemy, angleToEnemy, enemyLastVelocity);
		dodgeRandomize(e.getEnergy(), distanceToEnemy);
		oldEnergy=e.getEnergy();	//update enemy energy
	}
	
	/**
	 * Function that sets bullet energy given enemy distance
	 * @param distanceToEnemy	Distance to enemy
	 */
	double bulletEnergy(double distanceToEnemy) {
		return Math.min(200 / distanceToEnemy, 3);
	}
	
	/**
	 * Function to control fire per turn and energy given to the bullets depending on the robots energy and enemy distance
	 * @param distanceToEnemy Distance to the enemy robot
	 */
	public void shootUntilLowLife(double distanceToEnemy) {
		
		if(getEnergy() < 25 && distanceToEnemy > 250) {		//Shoot only every 10 rounds to increase survival
			if(countTurns%10 == 0) {
				setFire(bulletEnergy(distanceToEnemy));
			}	
		}else if(getEnergy() > 25 && distanceToEnemy > 250) {	//Not that risky, fire at will
			setFire(bulletEnergy(distanceToEnemy));
		}else if(getEnergy() < 25 && distanceToEnemy <= 250 && distanceToEnemy > 150) {	//Shoot only every 5 rounds to increase survival
			if(countTurns%5 == 0) {
				setFire(bulletEnergy(distanceToEnemy));
			}	
		}else if(getEnergy() > 25 && distanceToEnemy <= 250 && distanceToEnemy > 150) {	//Not that risky, fire at will
			setFire(bulletEnergy(distanceToEnemy));
		}
		
	}
	
	/**
	 * Function to handle robot movement
	 * 
	 * @param distanceToEnemy Distance to the enemy robot
	 * @param angleToEnemy angle to the enemy robot
	 * @param enemyLastVelocity enemy later velocity
	 */
	public void moveAndShoot(double distanceToEnemy, double angleToEnemy, double enemyLastVelocity) {
		
		if(distanceToEnemy > 250) {
			setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(angleToEnemy-getHeadingRadians()+enemyLastVelocity/getVelocity()));	//drive towards the enemie predicted future location				
			setAhead((distanceToEnemy - 140)*moveDirection);	//move forward
			shootUntilLowLife(distanceToEnemy);
		}if(distanceToEnemy <= 250 && distanceToEnemy > 150) {
			setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(angleToEnemy-getHeadingRadians()+enemyLastVelocity/getVelocity()));	//drive towards the enemie predicted future location				
			setAhead((distanceToEnemy - 140)*moveDirection);	//move forward
			shootUntilLowLife(distanceToEnemy);
		}else if(distanceToEnemy <= 150) {
			setTurnLeft(-90-distanceToEnemy);	//turn perpendicular to the enemy
			setAhead((distanceToEnemy + 160)*moveDirection);	//move forward
			setFire(bulletEnergy(distanceToEnemy));	//when close shoot no matter what
			wallsmoothing(angleToEnemy);	//When close to enemy add wallsmoothing to avoid walls
		}
		
	}
	
	/**
	 * A method for avoiding collisions with walls without needing to reverse direction.
	 * instead of reversing direction every time the robot would hit the wall
	 * the robot will instead turn as it approaches the wall
	 * then move right along the wall.
	 * 
	 * @param angleToEnemy angle to the enemy robot
	 */
	public void wallsmoothing(double angleToEnemy) {
		
		double objective = angleToEnemy-Math.PI/2*moveDirection;
		Rectangle2D mapRectangle = new Rectangle2D.Double(18, 18, getBattleFieldWidth()-36, getBattleFieldHeight()-36);	//Create rectangle equal to map size
		
		while(!mapRectangle.contains(getX()+Math.sin(objective)*120, getY()+ Math.cos(objective)*120)) {	//If map doesn't contain goal direction
			objective -= moveDirection*0.5;	//Decrease angle just enough until map contains goal direction
		}
		
		double smoothturn = robocode.util.Utils.normalRelativeAngle(objective-getHeadingRadians());
		
		if (Math.abs(smoothturn) > Math.PI/2){
			smoothturn = robocode.util.Utils.normalRelativeAngle(smoothturn + Math.PI);
		}
		
		turnRandomize(smoothturn);
	}
	
	/**
	 * Mainly randomizes angle given by wallsmoothing every 25 rounds
	 * @param turnAngle	angle given by wallsmoothing function
	 */
	public void turnRandomize(double turnAngle) {
			
		if(countTurns%25 == 0) {
			if(Math.random()>.50){
				setTurnRightRadians(turnAngle);
			}else {
				setTurnRightRadians(turnAngle);
			}
		}
			
	}
	
	/**
	 *This method is called when your robot collides with a wall.
	 *Because this method is called a lot of times in some particular cases
	 *the movement direction is only reversed when the turn is a multiple of 5
	 */
	public void onHitWall(HitWallEvent e){
		if(countTurns%5 == 0) {
			moveDirection=-moveDirection;
		}
	}
	
	/**
	 *This method is called when your robot collides with another robot.
	 *Because this method is called a lot of times in some particular cases
	 *the movement direction is only reversed when the turn is a multiple of 5
	 */
	public void onHitRobot(HitRobotEvent e) {
		if(countTurns%5 == 0) {
			moveDirection=-moveDirection;
		}
	}

	/**
	 * Function to handle movement when the enemy robot shoots
	 * Mainly randomizes movement direction every time that the enemy's robot energy decreases
	 * @param enemyEnergy	Enemy latest energy
	 * @param distanceToEnemy	Distance to the enemy
	 */
	public void dodgeRandomize(double enemyEnergy, double distanceToEnemy) {
		
		if(countTurns%20 == 0) {
			if(oldEnergy-enemyEnergy<=3&&oldEnergy-enemyEnergy>=0.1){
				if(Math.random()>.65){
					moveDirection =-moveDirection;
					setTurnLeft(-90-distanceToEnemy);
				}else {
					moveDirection =-moveDirection;
					setTurnRight(-90-distanceToEnemy);
				}
			}
		}
		
	}
	
}
