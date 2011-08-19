function newImage( imgSrc )
{
	var imgName = new Image();
	imgName.src = imgSrc;
	return imgName;
}

function change( imgName, imgLocation )
{
	document[imgName].src = imgLocation ;
}

var preloadFlag = false;
function loadImages()
{
	if (document.images)
	{
		home_over = newImage("images/home2.png");
		preloadFlag = true;
	}
}