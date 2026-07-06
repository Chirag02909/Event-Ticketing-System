import { useState, useEffect } from 'react';

/**
 * Custom hook to dynamically load the Razorpay checkout script.
 * Returns true if the script has been successfully loaded.
 */
export const useRazorpay = () => {
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    // If Razorpay is already available on window, set loaded immediately
    if (window.Razorpay) {
      setLoaded(true);
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => {
      console.log('Razorpay SDK loaded successfully');
      setLoaded(true);
    };
    script.onerror = () => {
      console.error('Failed to load Razorpay SDK');
    };

    document.body.appendChild(script);

    return () => {
      // Optional: Cleanup script if necessary, though keeping it is generally fine
    };
  }, []);

  return loaded;
};

export default useRazorpay;
