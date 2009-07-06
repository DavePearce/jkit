public class Vector3D {
	public double x = 0, y = 0, z = 0;
	
	public Vector3D()
	{
		x = y = z = 0;
	}
	
	public Vector3D(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void setVector(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void normalise()
	{
		double invlen = 1.0/length();
		
		if(invlen != 0.0)
		{
			x *= invlen;
			y *= invlen;
			z *= invlen;
		}
	}
	
	public double dot(Vector3D d)
	{
		return x*d.x + y*d.y + z*d.z;
	}
	
	public double length()
	{
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	public void clipMax(double x, double y, double z)
	{
		this.x = Math.min(this.x, x);
		this.y = Math.min(this.y, y);
		this.z = Math.min(this.z, z);
	}
	
	public void clipMin(double x, double y, double z)
	{
		this.x = Math.max(this.x, x);
		this.y = Math.max(this.y, y);
		this.z = Math.max(this.z, z);
	}
	
	public Vector3D times(double amount)
	{
		return new Vector3D(x*amount, y*amount, z*amount);
	}
	
	public Vector3D times(Vector3D other)
	{
		return new Vector3D(x*other.x, y*other.y, z*other.z);
	}
	
	public void multiply(double amount)
	{
		x *= amount;
		y *= amount;
		z *= amount;
	}
	
	public void multiply(Vector3D other)
	{
		x *= other.x;
		y *= other.y;
		z *= other.z;
	}
	
	public void add(Vector3D other)
	{
		this.x += other.x;
		this.y += other.y;
		this.z += other.z;
	}
	
	public void add(double amount)
	{
		x += amount;
		y += amount;
		z += amount;
	}
	
	public Vector3D plus(Vector3D other)
	{
		return new Vector3D(this.x + other.x, this.y + other.y, this.z + other.z);
	}
	
	public void subtract(Vector3D other)
	{
		this.x -= other.x;
		this.y -= other.y;
		this.z -= other.z;
	}
	
	public Vector3D minus(Vector3D other)
	{
		return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
	}
	
	public Vector3D clone()
	{
		return new Vector3D(x, y, z);
	}
}
