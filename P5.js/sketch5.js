function setup() {
    createCanvas(600, 600);
    angleMode(DEGREES);
    noFill();
    stroke(255);
  }
  
  function draw() {
    background(0);
    translate(width / 2, height / 2);
    beginShape();
    for (let theta = 0; theta < 360; theta++) {
      let k = mouseX / 100;
      let r = 200 * cos(k * theta);
      let x = r * cos(theta);
      let y = r * sin(theta);
      vertex(x, y);
    }
    endShape(CLOSE);
  }
  