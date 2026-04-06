let particles = [];

function setup() {
  createCanvas(windowWidth, windowHeight);
  for (let i = 0; i < 150; i++) {
    particles.push(new Particle());
  }
  stroke(255, 100);
  strokeWeight(1);
  noFill();
}

function draw() {
  background(10, 20, 30);

  for (let i = 0; i < particles.length; i++) {
    particles[i].update();
    particles[i].show();

    // Connect with nearby particles
    for (let j = i + 1; j < particles.length; j++) {
      let d = dist(particles[i].pos.x, particles[i].pos.y, particles[j].pos.x, particles[j].pos.y);
      if (d < 100) {
        stroke(255, map(d, 0, 100, 255, 0));
        line(particles[i].pos.x, particles[i].pos.y, particles[j].pos.x, particles[j].pos.y);
      }
    }
  }
}

// Resize canvas dynamically
function windowResized() {
  resizeCanvas(windowWidth, windowHeight);
}

class Particle {
  constructor() {
    this.pos = createVector(random(width), random(height));
    this.vel = p5.Vector.random2D();
    this.vel.mult(random(0.5, 2));
    this.size = random(2, 4);
  }

  update() {
    this.pos.add(this.vel);

    // Bounce off walls
    if (this.pos.x < 0 || this.pos.x > width) this.vel.x *= -1;
    if (this.pos.y < 0 || this.pos.y > height) this.vel.y *= -1;
  }

  show() {
    noStroke();
    fill(255);
    circle(this.pos.x, this.pos.y, this.size);
  }
}
