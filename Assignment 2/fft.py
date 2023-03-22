import argparse

# Define the command-line arguments
parser = argparse.ArgumentParser(description='FFT application')
parser.add_argument('-m', '--mode', type=int, default=1, choices=[1, 2, 3, 4],
                    help='the mode of operation')
parser.add_argument('-i', '--image', type=str, default='moonlanding.png',
                    help='filename of the image to process')

# Parse the command-line arguments
args = parser.parse_args()

# Print the parsed arguments
print(args.mode)
print(args.image)