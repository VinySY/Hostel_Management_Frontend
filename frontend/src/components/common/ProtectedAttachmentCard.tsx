import React, { useEffect, useState } from 'react';
import { AlertCircle, Download, ExternalLink, FileText, Loader2 } from 'lucide-react';
import { EvidenceFile } from '../../types';
import { apiService } from '../../services/api';
import { extractProtectedFileName } from '../../utils/fileAccess';

interface ProtectedAttachmentCardProps {
  attachment: EvidenceFile;
  compact?: boolean;
}

/**
 * Renders a complaint evidence attachment using JWT-authenticated fetching and Blob URLs.
 * Automatically revokes created Blob URLs on unmount or attachment change.
 */
export const ProtectedAttachmentCard: React.FC<ProtectedAttachmentCardProps> = ({
  attachment,
  compact = false,
}) => {
  const isImage = attachment.type?.toLowerCase().startsWith('image/') ?? false;
  const isProtected = Boolean(extractProtectedFileName(attachment.url));

  const [blobUrl, setBlobUrl] = useState<string | null>(() =>
    isProtected ? null : attachment.url || null
  );
  const [isLoading, setIsLoading] = useState<boolean>(isProtected && isImage);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    let createdUrl: string | null = null;
    const controller = new AbortController();

    if (!attachment.url) {
      setBlobUrl(null);
      setIsLoading(false);
      setError(null);
      return;
    }

    if (!isProtected) {
      setBlobUrl(attachment.url);
      setIsLoading(false);
      setError(null);
      return;
    }

    setBlobUrl(null);
    setError(null);

    if (isImage) {
      setIsLoading(true);
      apiService
        .fetchProtectedFile(attachment.url, controller.signal)
        .then((blob) => {
          if (cancelled) return;
          createdUrl = URL.createObjectURL(blob);
          setBlobUrl(createdUrl);
          setIsLoading(false);
        })
        .catch((err: unknown) => {
          if (cancelled || (err instanceof DOMException && err.name === 'AbortError')) return;
          setError(err instanceof Error ? err.message : 'Unable to load attachment.');
          setIsLoading(false);
        });
    } else {
      setIsLoading(false);
    }

    return () => {
      cancelled = true;
      controller.abort();
      if (createdUrl) {
        URL.revokeObjectURL(createdUrl);
      }
    };
  }, [attachment.url, isImage, isProtected]);

  // Also revoke on-demand blob URLs (for non-image files fetched on click)
  useEffect(() => {
    if (!isProtected || isImage || !blobUrl) return;
    return () => {
      URL.revokeObjectURL(blobUrl);
    };
  }, [blobUrl, isImage, isProtected]);

  const ensureBlobUrl = async (): Promise<string | null> => {
    if (blobUrl) return blobUrl;
    if (!attachment.url) return null;

    setIsLoading(true);
    setError(null);
    try {
      const blob = await apiService.fetchProtectedFile(attachment.url);
      const created = URL.createObjectURL(blob);
      setBlobUrl(created);
      return created;
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load attachment.');
      return null;
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpen = async () => {
    const resolved = await ensureBlobUrl();
    if (resolved) {
      window.open(resolved, '_blank', 'noopener,noreferrer');
    }
  };

  const handleDownload = async () => {
    const resolved = await ensureBlobUrl();
    if (!resolved) return;
    const link = document.createElement('a');
    link.href = resolved;
    link.download = attachment.name || 'attachment';
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  return (
    <div
      className={`${
        compact ? 'p-2.5 bg-white text-xs' : 'p-3 bg-slate-50'
      } border border-slate-200 rounded-lg space-y-2`}
    >
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center space-x-2 min-w-0">
          {isImage && blobUrl && !error ? (
            <img
              src={blobUrl}
              alt={attachment.name}
              className="w-8 h-8 rounded object-cover border border-slate-200 flex-shrink-0"
            />
          ) : (
            <FileText className="w-4 h-4 text-blue-500 flex-shrink-0" />
          )}
          <span className="text-xs font-medium text-slate-700 truncate">{attachment.name}</span>
        </div>

        {attachment.url && (
          <div className="flex items-center space-x-2 flex-shrink-0">
            <button
              type="button"
              onClick={handleOpen}
              disabled={isLoading}
              className="text-xs text-blue-600 hover:underline flex items-center cursor-pointer disabled:opacity-50"
            >
              {isLoading ? (
                <Loader2 className="w-3.5 h-3.5 mr-1 animate-spin" />
              ) : (
                <ExternalLink className="w-3.5 h-3.5 mr-1" />
              )}
              View
            </button>
            <button
              type="button"
              onClick={handleDownload}
              disabled={isLoading}
              title="Download file"
              className="text-xs text-slate-500 hover:text-slate-800 flex items-center cursor-pointer disabled:opacity-50"
            >
              <Download className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </div>

      {error && (
        <div className="flex items-center text-[11px] text-red-600 bg-red-50 border border-red-100 rounded px-2 py-1">
          <AlertCircle className="w-3.5 h-3.5 mr-1.5 flex-shrink-0" />
          <span className="truncate">{error}</span>
        </div>
      )}
    </div>
  );
};

