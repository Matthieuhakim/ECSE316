import argparse
import numpy as np
import matplotlib.colors as colors
import matplotlib.pyplot as plt
import time



# Define the command-line arguments
def parse_args():
    parser = argparse.ArgumentParser(description='FFT application')
    parser.add_argument('-m', '--mode', type=int, default=1, choices=[1, 2, 3, 4],
                        help='the mode of operation')
    parser.add_argument('-i', '--image', default='moonlanding.png',
                        help='filename of the image to process')

    # Parse the command-line arguments
    args = parser.parse_args()
    return args


# Define the DFT function
def naive_dft(x):
    x = np.asarray(x, dtype=complex)
    N = x.shape[0]
    n = np.arange(N)
    k = n.reshape((N, 1))
    
    M = np.exp(-2j * np.pi * k * n / N)
    
    return np.dot(M, x)

#Define the IDFT function
def naive_idft(x):
    x = np.asarray(x, dtype=complex)
    N = x.shape[0]
    n = np.arange(N)
    k = n.reshape((N, 1))
    
    M = np.exp(2j * np.pi * k * n / N)
    
    return np.dot(M, x) * 1 / N


# Define the FFT function with base case 16
def fft(x):
    x = np.asarray(x, dtype=complex)
    N = x.shape[0]
    
    if N % 2 > 0:
        raise ValueError("size of x must be a power of 2")
    elif N <= 16:  # this cutoff could be optimized
        return naive_dft(x)
    else:
        X_even = fft(x[::2])
        X_odd = fft(x[1::2])
        factor = np.exp(-2j * np.pi * np.arange(N) / N)
        return np.concatenate([X_even + factor[:int(N/2)] * X_odd,
                               X_even + factor[int(N/2):] * X_odd])


# Define the IFFT function with base case 16
def ifft(x):
    x = np.asarray(x, dtype=complex)
    N = x.shape[0]
    
    if N % 2 > 0:
        raise ValueError("size of x must be a power of 2")
    elif N <= 16:  # this cutoff could be optimized
        return naive_idft(x)
    else:
        X_even = ifft(x[::2])
        X_odd = ifft(x[1::2])
        
        x = np.zeros(N, dtype=complex)

        for n in range(N):
            x[n] = (N//2) * X_even[n % (N//2)] + \
                np.exp(2j * np.pi * n / N) * (N//2) * X_odd[n % (N//2)]
            x[n] /= N
        return x

# Define the 2DFFT function
def fft2d(x):
    x = np.asarray(x, dtype=complex)
    N, M = x.shape
    X = np.zeros((N, M), dtype=complex)

    for i in range(N):
        X[i, :] = fft(x[i, :])

    for j in range(M):
        X[:, j] = fft(X[:, j])

    return X
# Define the naive 2D fourier function
def naive_dft2d(x):
    x = np.asarray(x, dtype=complex)
    N, M = x.shape
    X = np.zeros((N, M), dtype=complex)

    for i in range(N):
        X[i, :] = naive_dft(x[i, :])

    for j in range(M):
        X[:, j] = naive_dft(X[:, j])

    return X
    

# Define the 2DIFFT function
def ifft2d(x):
    
    x = np.asarray(x, dtype=complex)
    N, M = x.shape
    X = np.zeros((N, M), dtype=complex)

    for i in range(N):
        X[i, :] = ifft(x[i, :])

    for j in range(M):
        X[:, j] = ifft(X[:, j])

    return X
     


# Helper function to find next closest power of 2
def nextpow2(x):
    return np.power(2, np.log2(x).astype(int) + 1)

# Helper function to resize image to next closest power of 2
def resize(image):
    newShape = nextpow2(image.shape[0]), nextpow2(image.shape[1])
    newImage = np.zeros(newShape)
    newImage[:image.shape[0], :image.shape[1]] = image
    return newImage

# Fast mode where the image is converted into its FFT form and displayed
def mode1(image):

    # Collect image data
    img = plt.imread(image).astype(float)

    # Resize image to next closest power of 2
    resizedImage = resize(img)

    # Apply FFT
    fftImage = fft2d(resizedImage)

    # Create plot for original image and FFT image
    fig = plt.figure()

    # Add figure for original image
    ax1 = fig.add_subplot(121)
    ax1.imshow(img, cmap='gray')
    ax1.set_title('Original Image')

    # Add figure for FFT image
    ax2 = fig.add_subplot(122)
    ax2.imshow(np.abs(fftImage), norm=colors.LogNorm(vmin=5))
    ax2.set_title('FFT Image')

    # Show both images
    plt.show()

    return 


# Denoising where the image is denoised by applying an FFT, truncating high frequencies and then displayed
def mode2(image):

    # Set pixel ratio to keep
    cutoff = 0.09

    # Collect image data
    img = plt.imread(image).astype(float)

    # Resize image to next closest power of 2
    resizedImage = resize(img)

    # Apply FFT
    fftImage = fft2d(resizedImage)
    rows, columns = fftImage.shape

    print("Pixel ratio is {}, we have ({}, {}) non-zeros out of ({}, {})".format(
        cutoff, int(cutoff * rows), int(cutoff * columns), rows, columns))

    # Truncate high frequencies
    fftImage[int(cutoff * rows) : int(rows * (1- cutoff))] = 0
    fftImage[:, int(cutoff * columns) : int(columns * (1- cutoff))] = 0

    # Apply IFFT
    ifftImage = ifft2d(fftImage).real

    # Truncate image to original size
    ifftImage = ifftImage[:img.shape[0], :img.shape[1]]

    # Create plot for original image and denoised image
    fig = plt.figure()

    # Add figure for original image
    ax1 = fig.add_subplot(121)
    ax1.imshow(img, cmap='gray')
    ax1.set_title('Original Image')

    # Add figure for denoised image
    ax2 = fig.add_subplot(122)
    ax2.imshow(ifftImage, cmap='gray')
    ax2.set_title('Denoised Image')

    # Show both images
    plt.show()

    return


# Compressing and saving the image
def mode3(image):

    # Take FFT of image
    img = plt.imread(image).astype(float)
    resizedImage = resize(img)
    fftImage = fft2d(resizedImage)

    # Define compression levels
    compressionLevels = [0, 20, 40, 60, 80, 95]

    # Create a 2 by 3 subplot
    fig, axs = plt.subplots(2, 3)

    # Loop through compression levels
    for compressionLevel in compressionLevels:
            
            # Threshold the coefficients’ magnitude and take only the largest percentile of them
            fftImageCompressed = fftImage.copy()
            fftImageCompressed[np.abs(fftImageCompressed) < np.percentile(np.abs(fftImageCompressed), compressionLevel)] = 0

            # Print number of non-zero coefficients
            print('Compression Level: ' + str(compressionLevel) + '%, Number of non-zero coefficients: ' + str(np.count_nonzero(fftImageCompressed)))

            # Save Fourier transform matrix of coefficients in a csv
            np.savetxt('fftImageCompressed' + str(compressionLevel) + '.csv', fftImageCompressed, delimiter=',')

            # Apply IFFT
            ifftImage = ifft2d(fftImageCompressed).real

            # Truncate image to original size
            ifftImage = ifftImage[:img.shape[0], :img.shape[1]]

            # Add image to subplot
            axs[compressionLevels.index(compressionLevel) // 3, compressionLevels.index(compressionLevel) % 3].imshow(ifftImage, cmap='gray')
            
            # Add title to subplot
            axs[compressionLevels.index(compressionLevel) // 3, compressionLevels.index(compressionLevel) % 3].set_title(str(compressionLevel) + '% Compression')

    # Show all images
    plt.show()
    return


# Plotting runtime graphs
def mode4():

    # Matrices rom 2^5 to 2^10
    sizes = [2**i for i in range(5, 11)]

    naive_times = []
    fft_times = []

    for size in sizes:

        # Naive method
        naive_time = []
        for i in range(10):
            matrix = np.random.rand(size, size)
            start = time.time()
            naive_dft2d(matrix)
            end = time.time()
            naive_time.append(end - start)
        naive_times.append(np.mean(naive_time))

        # FFT method
        fft_time = []
        for i in range(10):
            matrix = np.random.rand(size, size)
            start = time.time()
            fft2d(matrix)
            end = time.time()
            fft_time.append(end - start)
        fft_times.append(np.mean(fft_time))

        # Print mean and variance for fft
        print("Size: {}, Mean: {}, Variance: {}".format(size, np.mean(fft_time), np.var(fft_time)))

    # Plot data
    fig, ax = plt.subplots()
    ax.errorbar(sizes, naive_times, yerr=2*np.std(naive_times), label='Naive Method')
    ax.errorbar(sizes, fft_times, yerr=2*np.std(fft_times), label='FFT Method')
    ax.set_xlabel('Problem Size')
    ax.set_ylabel('Runtime (Seconds)')
    ax.set_title('2D FFT Runtimes')
    ax.legend()
    plt.show()
    
    return

# Main function
def main():
    args = parse_args()
    mode = args.mode
    image = args.image

    # Call corresponding mode function
    print("Mode", mode)
    if mode == 1:
        mode1(image)
    elif mode == 2:
        mode2(image)
    elif mode == 3:
        mode3(image)
    elif mode == 4:
        mode4()

main()


# randomly_generated_array = np.random.rand(64)
# randomly_generated_array_2D = np.random.rand(64,64)
# print(np.allclose(fft(randomly_generated_array), np.fft.fft(randomly_generated_array)))
# print(np.allclose(fft2d(randomly_generated_array_2D), np.fft.fft2(randomly_generated_array_2D)))
# print(np.allclose(naive_dft(randomly_generated_array), np.fft.fft(randomly_generated_array)))
# print(np.allclose(naive_dft2d(randomly_generated_array_2D), np.fft.fft2(randomly_generated_array_2D)))

# randomly_generated_array = np.random.rand(64)
# randomly_generated_array_2D = np.random.rand(64,64)
# print(np.allclose(randomly_generated_array, ifft(np.fft.fft(randomly_generated_array))))
# print(np.allclose(randomly_generated_array, naive_idft(naive_dft(randomly_generated_array))))
# print(np.allclose(randomly_generated_array_2D, ifft2d(np.fft.fft2(randomly_generated_array_2D))))