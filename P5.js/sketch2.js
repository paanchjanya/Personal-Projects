let angle;
let len = 120;

function setup() {
  createCanvas(800, 600);
  angleMode(DEGREES);
}

function draw() {
  background(51);
  stroke(255);
  translate(width / 2, height);
  angle = map(mouseX, 0, width, 0, 90);
  branch(len);
}

function branch(len) {
  strokeWeight(map(len, 2, 120, 1, 10));
  line(0, 0, 0, -len);
  translate(0, -len);
  if (len > 8) {
    push();
    rotate(angle);
    branch(len * 0.67);
    pop();
    push();
    rotate(-angle);
    branch(len * 0.67);
    pop();
  }
}
