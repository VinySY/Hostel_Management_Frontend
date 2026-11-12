import React, { useState, useEffect } from 'react';
import { useProtectedFileUrl } from '../../hooks/useProtectedFileUrl';

interface ProtectedImageProps {
  src?: string | null;
  fallbackSrc?: string;
  fallbackNode?: React.ReactNode;
  alt: string;
  className?: string;
}

/**
 * Renders an <img> for both public URLs and JWT-protected `/api/files/{name}` endpoints.
 * Falls back cleanly on loading, unauthorized, or missing-file errors.
 */
export const ProtectedImage: React.FC<ProtectedImageProps> = ({
  src,
  fallbackSrc,
  fallbackNode,
  alt,
  className = '',
}) => {
  const { objectUrl, isLoading, error } = useProtectedFileUrl(src);
  const [imgFailed, setImgFailed] = useState(false);

  useEffect(() => {
    setImgFailed(false);
  }, [src, objectUrl]);

  if (isLoading) {
    if (fallbackNode) {
      return <>{fallbackNode}</>;
    }
    if (fallbackSrc) {
      return <img src={fallbackSrc} alt={alt} className={className} />;
    }
    return <div className={`animate-pulse bg-slate-200 ${className}`} aria-label={alt} />;
  }

  const effectiveSrc = !error && !imgFailed ? objectUrl : null;
  if (effectiveSrc) {
    return (
      <img
        src={effectiveSrc}
        alt={alt}
        className={className}
        onError={() => setImgFailed(true)}
      />
    );
  }

  if (fallbackNode) {
    return <>{fallbackNode}</>;
  }

  if (fallbackSrc) {
    return <img src={fallbackSrc} alt={alt} className={className} />;
  }

  return null;
};

