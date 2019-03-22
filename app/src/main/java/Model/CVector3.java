package Model;

public class CVector3
{
    public float mX;
    public float mY;
    public float mZ;
    public float mT;

    public CVector3()
    {

    }
    public CVector3(float x, float y, float z, float t)
    {
        mX = x;
        mY = y;
        mZ = z;
        mT = t;
    }
    public CVector3(CVector3 r)
    {
        mX = r.mX;
        mY = r.mY;
        mZ = r.mZ;
        mT = r.mT;
    }
    public void setVal(float x, float y, float z,float t)
    {
        mX = x;
        mY = y;
        mZ = z;
        mT = t;
    }

    public float getMag()
    {
        return (float) Math.sqrt(mX * mX + mY * mY + mZ * mZ);
    }
}

