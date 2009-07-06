package raytracer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class Main
{
	private Vector<Object3D> objects = new Vector<Object3D>();

	private Vector<Light> lights = new Vector<Light>();

	public static final double epsilon = 0.0000001;

	private Vector3D bgColour = new Vector3D(0, 0, 0);

	private Vector3D ambientColour = new Vector3D(0, 0, 0);

	public static void main(String[] argv)
	{
		Frame frame = new Frame("GeoffTrace");
		Window win = new Window(frame);

		try
		{
			int screenWidth = 320;//200;
			int screenHeight = 240;//180;			

			frame.setSize(screenWidth, screenHeight);
			frame.setBounds(4, 26, screenWidth+4, screenHeight+26);
			frame.validate();
			frame.setVisible(true);

			Graphics graphicwindow = frame.getGraphics();
			graphicwindow.fillRect(0, 0, screenWidth, screenHeight);

			BufferedImage offscreen = new BufferedImage(screenWidth,screenHeight, BufferedImage.TYPE_INT_RGB);
			Graphics offscreen_high = offscreen.createGraphics();

			offscreen_high.setColor(Color.black);
			offscreen_high.fillRect(0, 0, screenWidth, screenHeight);

			Main m = new Main();

			m.bgColour.y = 0.4;
			m.bgColour.z = 1;
			m.ambientColour.x = m.ambientColour.y = m.ambientColour.z = 0.005;

			Sphere s = new Sphere(new Vector3D(0, 0, 20), 5);
			s.colour.setVector(1,0,0);
			s.reflectivity = 0.05;
			s.fresnelReflection = true;
			m.objects.add(s);

			s = new Sphere(new Vector3D(1.5, -5, 14.5), 1);
			s.colour.setVector(0,1,0);
			m.objects.add(s);

			s = new Sphere(new Vector3D(12, 0, 30), 0.2);
			s.colour.setVector(1,1,0);
			m.objects.add(s);

			Plane p = new Plane(new Vector3D(0, 5, 0), new Vector3D(0,-1,0));
			//p.reflectivity = 0.9;
			m.objects.add(p);

			Light l = new Light(new Vector3D(20, -20, -10), new Vector3D(1,1,1));
			m.lights.add(l);

			//Light l2 = new Light(new Vector3D(-20, 20, 35), new Vector3D(1,1,1));
			// m.lights.add(l2);
			
			double spp = 1;
			double sqrtAmount = Math.sqrt(spp);
			double oneOverSqrtAmount = 1.0/sqrtAmount;
			double halfOneOverSqrtAmount = oneOverSqrtAmount*0.5;
			
			long time = System.currentTimeMillis();
			long time2 = time;

			for(int x = 0; x < 20; x++)
			{
				for(int i = 0; i < screenWidth; i++)
				{
					for(int j = 0; j < screenHeight; j++)
					{
						Vector3D colour = new Vector3D(0, 0, 0);
						Vector3D dir;

						// antialiased goodness
						for(int a = 0; a < spp; a++)
						{
							//dir = new Vector3D(((i+(a%2)*0.5+0.25)-screenWidth/2.0)/(double)screenWidth, ((j+(double)(int)(a/2)*0.5+0.25)-screenHeight/2.0)/(double)screenHeight*(screenHeight/(double)screenWidth), 1);
							dir = new Vector3D(((i+(a%sqrtAmount)*oneOverSqrtAmount+halfOneOverSqrtAmount)-screenWidth/2.0)/(double)screenWidth, ((j+(double)(int)(a/sqrtAmount)*oneOverSqrtAmount+halfOneOverSqrtAmount)-screenHeight/2.0)/(double)screenHeight*(screenHeight/(double)screenWidth), 1);
							dir.normalise();
							colour.add(m.trace(new Ray(new Vector3D(0,-1,0), dir), 0));
						}

						colour.multiply(1.0/spp);
						
						//dir = new Vector3D((i-screenWidth/2.0)/(double)screenWidth, (j-screenHeight/2.0)/(double)screenHeight*(screenHeight/(double)screenWidth), 1);
						//dir.normalise();
						//colour.add(m.trace(new Ray(new Vector3D(0,0,0), dir), 0));
						
						//colour.x = Math.pow(colour.x, 1.0/2.5);
						//colour.y = Math.pow(colour.y, 1.0/2.5);
						//colour.z = Math.pow(colour.z, 1.0/2.5);
						
						colour.clipMax(1,1,1);
						
						//colour.x = Math.random();

						offscreen.setRGB(i,j,((int)(colour.z*255)) + ((int)(colour.y*255))*256 + ((int)(colour.x*255))*256*256);
					}
				}

				graphicwindow.drawImage(offscreen, 4, 26, null);

				//if(m.objects.get(1) != null)
				((Sphere)m.objects.get(1)).center.y += 0.5;
				((Sphere)m.objects.get(2)).radius += 0.15;
				m.lights.get(0).pos.y += 1;
				
				m.bgColour.subtract(new Vector3D(0.05, 0.05, 0.05));
				m.bgColour.clipMin(0, 0, 0);
				
				System.out.println(System.currentTimeMillis()-time2);
				time2 = System.currentTimeMillis();
			}
			
			System.out.println("Total time: " + (System.currentTimeMillis() - time) + " ms");
		}
		catch (Throwable e)
		{
		}
		
		frame.dispose();
	}

	public Vector3D trace(Ray ray, int depth)
	{
		Vector3D colour = new Vector3D(0, 0, 0);

		double closestDistance = Double.MAX_VALUE;
		Object3D closest = null;
		HitResult Result = new HitResult();

		for(Object3D s : objects)
		{
			HitResult result = s.intersect(ray);

			if(result.hit && result.distance >= 0.0)
			{
				if(result.distance < closestDistance)
				{
					closestDistance = result.distance;
					Result = result;
					closest = s;
				}
			}
		}
		
		Vector3D lightColour = new Vector3D(0,0,0);

		if(Result.hit)
		{
			Vector3D intersection = Result.intersection;
			intersection.add(Result.normal.times(epsilon));

			for(Light l : lights)
			{
				Vector3D lightDir = l.pos.minus(intersection);
				double lightDist = lightDir.length();
				lightDir.normalise();

				Ray lightRay = new Ray(intersection, lightDir);
				HitResult Result2 = new HitResult();

				for(Object3D s : objects)
				{
					HitResult result = s.intersect(lightRay);

					if(result.hit && result.distance >= 0.0 && result.distance < lightDist)
					{
						Result2 = result;
						break;
					}
				}

				if(Result2.hit)
					continue;

				
				double dot = Result.normal.dot(lightDir);

				if(dot < 0.0)
					continue;

				lightColour.add(l.colour.times(dot).times(closest.colour));

				//Vector3D r = lightDir.plus(ray.direction.times(-1)).times(0.5);
				Vector3D r = Result.normal.times(2*Result.normal.dot(ray.direction)).minus(ray.direction);
				r.multiply(-1);

				//double s = Result.normal.dot(r)*1.1;
				double s = lightDir.dot(r);

				if(s > 0)
					lightColour.add(Math.pow(s, 100));
			}
			
			
			lightColour.add(ambientColour.times(closest.colour));
			
			colour.add(lightColour);		
			
			Vector3D reflectColour = new Vector3D(0,0,0);
			double reflectAmount = closest.reflectivity;
			
			if(depth < 3 && reflectAmount > epsilon)
			{
				if(closest.fresnelReflection)
				{
					reflectAmount = Math.min(reflectAmount+1+Result.normal.dot(ray.direction), 1);
					reflectAmount *= reflectAmount;
				}
				
				Vector3D reflectVector = Result.normal.times(2*(Result.normal.dot(ray.direction))).minus(ray.direction);
				reflectVector.multiply(-1);
				reflectColour.add(trace(new Ray(intersection, reflectVector), depth+1));
			}

			return colour.times(1-reflectAmount).plus(reflectColour.times(reflectAmount));
		}
		else
			return bgColour;
	}
}
