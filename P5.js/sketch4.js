let grid, next;
let dA = 1.0, dB = 0.5, feed = 0.055, k = 0.062;
let resolution = 2;

function setup() {
  createCanvas(400, 400);
  pixelDensity(1);
  grid = [];
  next = [];

  for (let x = 0; x < width / resolution; x++) {
    grid[x] = [];
    next[x] = [];
    for (let y = 0; y < height / resolution; y++) {
      grid[x][y] = { a: 1, b: 0 };
      next[x][y] = { a: 1, b: 0 };
    }
  }

  // Seed B in the center
  for (let i = 100; i < 110; i++) {
    for (let j = 100; j < 110; j++) {
      grid[i][j].b = 1;
    }
  }
}

function draw() {
  background(51);
  for (let x = 1; x < grid.length - 1; x++) {
    for (let y = 1; y < grid[x].length - 1; y++) {
      let a = grid[x][y].a;
      let b = grid[x][y].b;

      next[x][y].a = a + (dA * laplaceA(x, y)) - (a * b * b) + (feed * (1 - a));
      next[x][y].b = b + (dB * laplaceB(x, y)) + (a * b * b) - ((k + feed) * b);

      next[x][y].a = constrain(next[x][y].a, 0, 1);
      next[x][y].b = constrain(next[x][y].b, 0, 1);
    }
  }

  loadPixels();
  for (let x = 0; x < grid.length; x++) {
    for (let y = 0; y < grid[x].length; y++) {
      let pix = (x + y * width / resolution) * 4;
      let c = floor((grid[x][y].a - grid[x][y].b) * 255);
      c = constrain(c, 0, 255);
      for (let i = 0; i < resolution; i++) {
        for (let j = 0; j < resolution; j++) {
          let px = (x * resolution + i + (y * resolution + j) * width) * 4;
          pixels[px + 0] = c;
          pixels[px + 1] = c;
          pixels[px + 2] = c;
          pixels[px + 3] = 255;
        }
      }
    }
  }
  updatePixels();

  let temp = grid;
  grid = next;
  next = temp;
}

function laplaceA(x, y) {
  let sumA = 0;
  sumA += grid[x][y].a * -1;
  sumA += grid[x - 1][y].a * 0.2;
  sumA += grid[x + 1][y].a * 0.2;
  sumA += grid[x][y + 1].a * 0.2;
  sumA += grid[x][y - 1].a * 0.2;
  sumA += grid[x - 1][y - 1].a * 0.05;
  sumA += grid[x + 1][y - 1].a * 0.05;
  sumA += grid[x + 1][y + 1].a * 0.05;
  sumA += grid[x - 1][y + 1].a * 0.05;
  return sumA;
}

function laplaceB(x, y) {
  let sumB = 0;
  sumB += grid[x][y].b * -1;
  sumB += grid[x - 1][y].b * 0.2;
  sumB += grid[x + 1][y].b * 0.2;
  sumB += grid[x][y + 1].b * 0.2;
  sumB += grid[x][y - 1].b * 0.2;
  sumB += grid[x - 1][y - 1].b * 0.05;
  sumB += grid[x + 1][y - 1].b * 0.05;
  sumB += grid[x + 1][y + 1].b * 0.05;
  sumB += grid[x - 1][y + 1].b * 0.05;
  return sumB;
}
