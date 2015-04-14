package test;

import com.example.circularimageview.CircularImageView;
import com.example.circularimageview.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		CircularImageView image=(CircularImageView)findViewById(R.id.image);
		Bitmap bm=BitmapFactory.decodeResource(getResources(), R.drawable.pic4);
		image.setImageBitmap(bm);
		
		/*Bitmap bm=BitmapFactory.decodeResource(getResources(), R.drawable.pic4);
		CircularImageView image1=new CircularImageView(this);
		image1.setImageBitmap(bm);
		MyLinearLayout linearLayout=new MyLinearLayout(this);
		linearLayout.addView(image1, new LayoutParams(400, 400));
		setContentView(linearLayout);*/
	}
	
	

}
