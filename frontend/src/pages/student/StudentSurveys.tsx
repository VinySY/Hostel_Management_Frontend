import React, { useState, useEffect } from 'react';
import { ClipboardList, Star, Send, CheckCircle2, AlertCircle, Info } from 'lucide-react';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { EmptyState } from '../../components/common/EmptyState';
import { apiService } from '../../services/api';
import { Survey, SurveySubmissionData } from '../../types';

const SURVEY_QUESTIONS = [
  { id: 'q1Cleanliness', title: '1. Hostel Cleanliness & Common Areas', desc: 'Corridors, staircases, common rooms, and campus grounds hygiene' },
  { id: 'q2Washrooms', title: '2. Washroom Hygiene & Sanitization', desc: 'Toilet cleanliness, regular disinfection, water pressure, and drainage' },
  { id: 'q3Furniture', title: '3. Room Furniture & Bed Quality', desc: 'Condition of beds, mattresses, study tables, chairs, and lockers' },
  { id: 'q4DrinkingWater', title: '4. RO Drinking Water Availability', desc: 'Water cooler functionality, filtration quality, and temperature' },
  { id: 'q5Wifi', title: '5. High-speed Wi-Fi & Connectivity', desc: 'Hostel Wi-Fi signal strength, bandwidth speed, and uptime' },
  { id: 'q6Electricity', title: '6. Electricity & Power Backup', desc: 'Room lights, sockets, ceiling fan/cooler/AC operation, and generator backup' },
  { id: 'q7Food', title: '7. Mess Food Quality & Dining Hygiene', desc: 'Nutritional balance, menu variety, taste, and mess hall sanitization' },
  { id: 'q8Security', title: '8. Security & Guard Vigilance', desc: 'Hostel gate security, visitor logging, nighttime surveillance, and safety' },
  { id: 'q9WardenSupport', title: '9. Warden Support & Responsiveness', desc: 'Accessibility of warden, grievance hearing, and empathetic resolution' },
  { id: 'q10ComplaintResolution', title: '10. Complaint Resolution Timeliness', desc: 'Speed and effectiveness in closing reported maintenance tickets' },
];

export const StudentSurveys: React.FC = () => {
  const [surveys, setSurveys] = useState<Survey[]>([]);
  const [selectedSurvey, setSelectedSurvey] = useState<Survey | null>(null);
  const [ratings, setRatings] = useState<Record<string, number>>({
    q1Cleanliness: 5,
    q2Washrooms: 5,
    q3Furniture: 5,
    q4DrinkingWater: 5,
    q5Wifi: 5,
    q6Electricity: 5,
    q7Food: 5,
    q8Security: 5,
    q9WardenSupport: 5,
    q10ComplaintResolution: 5,
  });
  const [comments, setComments] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchSurveys = async () => {
    setIsLoading(true);
    try {
      const data = await apiService.getActiveSurveys();
      setSurveys(data);
      if (data.length > 0 && !selectedSurvey) {
        // Default to first active survey not yet submitted, if any
        const unsubmitted = data.find((s) => !s.hasSubmitted);
        setSelectedSurvey(unsubmitted || data[0]);
      }
    } catch (err) {
      console.error('Failed to load surveys:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSurveys();
  }, []);

  const getWordCount = (text: string) => {
    const trimmed = text.trim();
    if (!trimmed) return 0;
    return trimmed.split(/\s+/).length;
  };

  const wordCount = getWordCount(comments);
  const isOverWordLimit = wordCount > 30;

  const handleRatingChange = (key: string, val: number) => {
    setRatings((prev) => ({ ...prev, [key]: val }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSurvey) return;
    if (isOverWordLimit) {
      setErrorMessage('Comments cannot exceed 30 words');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    const payload: SurveySubmissionData = {
      q1Cleanliness: ratings.q1Cleanliness,
      q2Washrooms: ratings.q2Washrooms,
      q3Furniture: ratings.q3Furniture,
      q4DrinkingWater: ratings.q4DrinkingWater,
      q5Wifi: ratings.q5Wifi,
      q6Electricity: ratings.q6Electricity,
      q7Food: ratings.q7Food,
      q8Security: ratings.q8Security,
      q9WardenSupport: ratings.q9WardenSupport,
      q10ComplaintResolution: ratings.q10ComplaintResolution,
      comments: comments.trim() || undefined,
    };

    try {
      await apiService.submitSurvey(selectedSurvey.id, payload);
      setSubmitSuccess(true);
      setComments('');
      await fetchSurveys();
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to submit survey feedback');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Student Feedback Surveys</h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Standard institutional assessment directly reviewed by the Director General of Hostels (DGM)
        </p>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-slate-400">Loading active surveys...</div>
      ) : surveys.length === 0 ? (
        <EmptyState
          title="No active surveys"
          description="There are currently no active feedback surveys open for your hostel."
        />
      ) : (
        <div className="space-y-6">
          {/* Survey Selector if multiple */}
          {surveys.length > 1 && (
            <div className="flex gap-2 overflow-x-auto pb-1">
              {surveys.map((s) => (
                <button
                  key={s.id}
                  onClick={() => {
                    setSelectedSurvey(s);
                    setSubmitSuccess(false);
                    setErrorMessage(null);
                  }}
                  className={`px-3 py-2 rounded-lg text-xs font-medium cursor-pointer transition-all flex items-center gap-2 border ${
                    selectedSurvey?.id === s.id
                      ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                      : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <ClipboardList className="w-3.5 h-3.5" />
                  <span className="truncate max-w-[200px]">{s.title}</span>
                  {s.hasSubmitted && (
                    <span className="px-1.5 py-0.2 bg-emerald-100 text-emerald-800 rounded-full text-[10px] font-semibold">
                      Submitted
                    </span>
                  )}
                </button>
              ))}
            </div>
          )}

          {selectedSurvey && (
            <Card className="!p-6 space-y-6">
              <div className="border-b border-slate-100 pb-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-bold text-slate-900">{selectedSurvey.title}</h3>
                  {selectedSurvey.hasSubmitted ? (
                    <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      Feedback Submitted
                    </span>
                  ) : (
                    <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                      Response Open
                    </span>
                  )}
                </div>
                {selectedSurvey.description && (
                  <p className="text-xs text-slate-600 mt-2">{selectedSurvey.description}</p>
                )}
                <div className="mt-2 flex items-center gap-2 text-[11px] text-slate-400">
                  <Info className="w-3.5 h-3.5 text-blue-500" />
                  <span>Ratings use a standard 1 (Poor) to 5 (Excellent) institutional scale.</span>
                </div>
              </div>

              {submitSuccess ? (
                <div className="p-8 text-center bg-emerald-50 border border-emerald-200 rounded-xl space-y-3">
                  <CheckCircle2 className="w-12 h-12 text-emerald-600 mx-auto" />
                  <h4 className="text-base font-bold text-emerald-900">Thank you for your feedback!</h4>
                  <p className="text-xs text-emerald-700 max-w-md mx-auto">
                    Your responses have been recorded and forwarded directly to the Director General of Hostels. Actionable issues are assigned to wardens for continuous quality improvements.
                  </p>
                </div>
              ) : selectedSurvey.hasSubmitted ? (
                <div className="p-8 text-center bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                  <CheckCircle2 className="w-10 h-10 text-slate-400 mx-auto" />
                  <h4 className="text-sm font-bold text-slate-800">You have already submitted this survey</h4>
                  <p className="text-xs text-slate-500 max-w-sm mx-auto">
                    Institutional policy permits one response per resident per survey cycle to maintain statistical integrity.
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-6">
                  {errorMessage && (
                    <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-xs text-red-700">
                      <AlertCircle className="w-4 h-4 flex-shrink-0 text-red-500" />
                      <span>{errorMessage}</span>
                    </div>
                  )}

                  <div className="space-y-4">
                    {SURVEY_QUESTIONS.map((q) => {
                      const currentVal = ratings[q.id] || 5;
                      return (
                        <div
                          key={q.id}
                          className="p-4 bg-slate-50/70 border border-slate-200/80 rounded-xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 hover:bg-slate-50 transition-colors"
                        >
                          <div className="space-y-0.5 max-w-md">
                            <h4 className="text-xs font-bold text-slate-900">{q.title}</h4>
                            <p className="text-[11px] text-slate-500">{q.desc}</p>
                          </div>

                          <div className="flex items-center gap-1.5 self-start sm:self-center">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <button
                                key={star}
                                type="button"
                                onClick={() => handleRatingChange(q.id, star)}
                                className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold transition-all cursor-pointer ${
                                  currentVal >= star
                                    ? 'bg-amber-400 text-amber-950 shadow-sm'
                                    : 'bg-white text-slate-400 border border-slate-200 hover:border-amber-300'
                                }`}
                                title={`${star} / 5`}
                              >
                                {star}
                              </button>
                            ))}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* 30-word max suggestion */}
                  <div className="p-4 bg-white border border-slate-200 rounded-xl space-y-2">
                    <div className="flex justify-between items-center">
                      <label className="text-xs font-bold text-slate-900">
                        Optional Feedback / Suggestion (Max 30 words)
                      </label>
                      <span
                        className={`text-[11px] font-mono font-semibold ${
                          isOverWordLimit ? 'text-red-600' : 'text-slate-400'
                        }`}
                      >
                        {wordCount} / 30 words
                      </span>
                    </div>
                    <textarea
                      value={comments}
                      onChange={(e) => setComments(e.target.value)}
                      placeholder="Share a concise, actionable note or specific maintenance observation..."
                      rows={3}
                      className={`w-full p-3 text-xs border rounded-lg focus:outline-none focus:ring-1 transition-all ${
                        isOverWordLimit
                          ? 'border-red-400 focus:ring-red-400 bg-red-50/30'
                          : 'border-slate-200 focus:ring-blue-500'
                      }`}
                    />
                    {isOverWordLimit && (
                      <p className="text-[11px] text-red-600 font-medium">
                        Please trim your comment to 30 words or fewer before submitting.
                      </p>
                    )}
                  </div>

                  <div className="flex justify-end pt-2">
                    <Button
                      type="submit"
                      variant="primary"
                      isLoading={isSubmitting}
                      disabled={isOverWordLimit}
                      icon={<Send className="w-3.5 h-3.5" />}
                      className="px-6"
                    >
                      Submit Feedback to DGM
                    </Button>
                  </div>
                </form>
              )}
            </Card>
          )}
        </div>
      )}
    </div>
  );
};
