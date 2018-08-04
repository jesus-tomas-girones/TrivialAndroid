var canvas;
var X, Y;
var miRuleta;

window.onload = function () {

    canvas = document.getElementById("canvas");
    inicializarCanvas();

    function inicializarCanvas() {
        var ctx = canvas.getContext("2d");
        var s = getComputedStyle(canvas);
        var w = s.width;
        var h = s.height;
        canvas.width = w.split("px")[0];
        canvas.height = h.split("px")[0];
        X = canvas.width / 2;
        Y = canvas.height / 2;

    }

    miRuleta = new Winwheel({
        'numSegments': 0, // Número de segmentos
        'outerRadius': Y - 20, // Radio externo
        'drawText': true,             // Code drawn text can be used with segment images.
        'textFontSize': 10,               // Set text options as desired.
//        'textOrientation': 'curved',
        'textAlignment': 'center',
//        'textMargin': Y - 50,
        'textFontFamily': 'monospace',
        'textLineWidth': 3,
        'lineWidth': 3,
        'textFillStyle': 'black',
        'strokeStyle': 'black',
        'imageOverlay': false,
//        'drawMode': 'segmentImage',
        'imageDirection'    : 'E' ,
        'animation': {
            'type': 'spinToStop', // Giro y alto
            'duration': 10,
            'spins': 5,
            'callbackSound': playSound,    // Specify function to call when sound is to be triggered
            'soundTrigger': 'pin',

            'callbackFinished': Mensaje, // Función para mostrar mensaje
            'callbackAfter': dibujarIndicador // Funciona de pintar indicador
        },
        'pins':    // Display pins, and if desired specify the number.
        {
            'number': 32
        }
    });

    //	    var audio = new Audio('tick.mp3');  // Create audio object and load desired file.
    miRuleta.draw();


    function playSound() {
        // Stop and rewind the sound (stops it if already playing).
        /*audio.pause();
        audio.currentTime = 0;

        // Play the sound.
        audio.play();*/
        jsInterfazNativa.playSound();
    }



    function Mensaje() {
        var SegmentoSeleccionado = miRuleta.getIndicatedSegment();
        //           alert("Elemento seleccionado:" + SegmentoSeleccionado.text + "!");
        miRuleta.stopAnimation(false);
        miRuleta.rotationAngle = 0;
        //miRuleta.draw();
        //dibujarIndicador();

        jsInterfazNativa.categorySelected(SegmentoSeleccionado.text);
    }




    function deleteSegment() {
        // Call function to remove a segment from the wheel, by default the last one will be
        // removed; you can pass in the number of the segment to delete if desired.
        miRuleta.deleteSegment();

        // The draw method of the wheel object must be called to render the changes.
        miRuleta.draw();
    }


}

function dibujarIndicador() {
    var ctx = miRuleta.ctx;
    ctx.strokeStyle = 'navy';
    ctx.fillStyle = 'black';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(X - 10, 0);
    ctx.lineTo(X + 10, 0);
    ctx.lineTo(X, 25);
    ctx.lineTo(X - 10, 0);
    ctx.stroke();
    ctx.fill();
}





function addSegment(text, color, imagen) {
    // Use a date object to set the text of each added segment to the current minutes:seconds
    // (just to give each new segment a different label).
    var date = new Date();

    // The Second parameter in the call to addSegment specifies the position,
    // in this case 1 meaning the new segment goes at the start of the wheel.
    miRuleta.addSegment({
        text: text,
        fillStyle: color
    }, 1);

    miRuleta.segments[1].text = text;
    miRuleta.segments[1].fillStyle = color;
//    miRuleta.segments[1].changeImage(imagen, imageDirection = null);
    //           console.log(text + ' ' + color  + ' ' +  imagen);

    // The draw method of the wheel object must be called in order for the changes
    // to be rendered.
    miRuleta.draw();
    dibujarIndicador();
}

function girar() {
    miRuleta.startAnimation();
}



